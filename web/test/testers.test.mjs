import {test} from 'node:test';
import assert from 'node:assert/strict';
import {onRequest,testerLinks} from '../functions/api/testers.js';

const env={TESTER_GROUP_URL:'https://groups.google.com/g/viajaconintu-satipo-testers',
  TESTER_PLAY_URL:'https://play.google.com/apps/testing/com.intu.taxi',TESTER_ACCESS_READY:'true',TESTER_GATEWAY_KEY:'a'.repeat(64)};
const request=(body, origin='https://viajaconintu.pages.dev',type='application/json')=>new Request('https://viajaconintu.pages.dev/api/testers',{
  method:'POST',headers:{Origin:origin,'Content-Type':type,'CF-Connecting-IP':'192.0.2.1'},body:typeof body==='string'?body:JSON.stringify(body),
});
test('links accept only the Intu test and genuine group origins',()=>{
  assert.ok(testerLinks(env));
  for (const bad of ['https://evil.example/g/testers','https://groups.google.com/g/testers?redirect=evil','https://groups.google.com/g/testers#evil'])
    assert.equal(testerLinks({...env,TESTER_GROUP_URL:bad}),null);
  assert.equal(testerLinks({...env,TESTER_PLAY_URL:'https://play.google.com/apps/testing/other.app'}),null);
});
test('closed configuration cannot advertise a working installation',async()=>{
  const response=await onRequest({request:new Request('https://viajaconintu.pages.dev/api/testers'),env:{}});
  assert.deepEqual(await response.json(),{enabled:false});
});
test('reject untrusted origins, missing consent, bad addresses and unbounded bodies before contacting storage',async()=>{
  const original=globalThis.fetch; globalThis.fetch=()=>{throw new Error('unexpected backend request');};
  try {
    assert.equal((await onRequest({request:request({email:'qa@example.com',consent:true},'https://evil.example'),env})).status,403);
    assert.equal((await onRequest({request:request({email:'qa@example.com',consent:false}),env})).status,400);
    assert.equal((await onRequest({request:request({email:'invalid',consent:true}),env})).status,400);
    assert.equal((await onRequest({request:request('x'.repeat(2049)),env})).status,413);
  } finally {globalThis.fetch=original;}
});
test('store normalized email with a salted IP digest, propagate limits and hide backend errors',async()=>{
  const original=globalThis.fetch; let input; let replyStatus=200;
  globalThis.fetch=async (url,opts)=>{
    input=JSON.parse(opts.body);
    return new Response(JSON.stringify(replyStatus===429?{message:'too_many_requests'}:replyStatus===503?{message:'private database details'}:{saved:true}),{status:replyStatus});
  };
  try {
    const response=await onRequest({request:request({email:' QA@EXAMPLE.COM ',consent:true}),env});
    assert.equal(response.status,200); assert.equal(input.p_email,'qa@example.com');
    assert.match(input.p_ip_hash,/^[a-f0-9]{64}$/); assert.equal(JSON.stringify(input).includes('192.0.2.1'),false);
    const publicReply=await response.text(); assert.equal(publicReply.includes(env.TESTER_GATEWAY_KEY),false);
    replyStatus=429; assert.equal((await onRequest({request:request({email:'qa@example.com',consent:true}),env})).status,429);
    replyStatus=503; assert.equal((await (await onRequest({request:request({email:'qa@example.com',consent:true}),env})).text()).includes('private database'),false);
  } finally {globalThis.fetch=original;}
});
