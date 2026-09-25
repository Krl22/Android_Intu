// Supabase acepta los tokens de Firebase solo si traen el claim `role: 'authenticated'`
// (así asigna el rol de Postgres `authenticated`, que es el que usan las políticas RLS).
// Estas funciones "blocking" lo agregan en cada registro e inicio de sesión.
// Requieren Firebase Authentication with Identity Platform (también necesario para MFA).
// Docs: https://supabase.com/docs/guides/auth/third-party/firebase-auth

const { beforeUserCreated, beforeUserSignedIn } = require('firebase-functions/v2/identity');

const supabaseClaims = { role: 'authenticated' };

exports.beforecreated = beforeUserCreated(() => ({ customClaims: supabaseClaims }));

exports.beforesignedin = beforeUserSignedIn(() => ({ customClaims: supabaseClaims }));
