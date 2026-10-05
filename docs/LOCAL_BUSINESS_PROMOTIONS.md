# Fotos y promociones locales del MVP

Los tres negocios son ficticios, con precios de ejemplo en soles. Ninguna promoción pertenece a un comercio real. Los pedidos simulan productos; solo el transporte usa el flujo de la moto.

## Estilo y ubicación

El carrusel adapta la referencia del usuario: tarjeta horizontal redondeada, oferta/precio a la izquierda, foto a la derecha e indicadores debajo. Conserva el degradado turquesa e índigo de Intu y admite modo oscuro. El menú muestra fotos, cantidades y subtotal; el envío se cotiza después de elegir destino.

Los puntos de prueba aproximan las capitales de Satipo y Río Negro según [INEI, división política de Junín](https://www.inei.gob.pe/media/MenuRecursivo/publicaciones_digitales/Est/Lib1108/Libro.pdf). El administrador debe verificar el punto con su repartidor antes de una prueba en campo. La preparación del juane toma como referencia la [gastronomía amazónica de PROMPERÚ](https://www.peru.travel/gastronomia/es/cocina-peruana/cocina-de-la-amazonia.html).

## Generación de imágenes

Proveedor: Higgsfield, herramienta generate_image, modelo gpt_image_2_5. Cuatro generaciones nuevas (sin imágenes de entrada), relación 1:1, una foto por solicitud, fondo opaco. Preflight: 0.25 créditos por foto, 1 crédito para cuatro solicitudes a esos parámetros. Las fotos se revisaron en el menú nativo y se incluyen como recursos locales para funcionar sin red.

| Archivo en app/src/main/res/drawable-nodpi | Generación |
| --- | --- |
| demo_chicken.png | 6f4ab045-fd89-4f47-b29c-62655e697938 |
| demo_juane.png | 7ba2177d-3587-43ce-af51-95224c0d1a11 |
| demo_coffee.png | 265efa20-c23a-49c1-b3a8-e9acf44c493e |
| demo_chaufa.png | 7df90bcd-c547-4c37-8c3d-974e58a22bf1 |

### Prompts exactos

**chicken**

Use case: product-mockup. Asset type: product photograph for a Peruvian delivery app promotion. Photorealistic appetizing quarter of Peruvian pollo a la brasa, golden skin with realistic charcoal roast marks, served with crisp French fries and a small fresh lettuce-tomato salad on a white ceramic plate, a small ají sauce cup next to the plate. Scene: clean warm ivory tabletop. Composition: tightly framed square commercial food photograph, overhead three-quarter view, entire plate visible with small margins, food centered, no people. Soft daylight, realistic appetizing texture and colors. No text, no brands, no logos, no typography, no watermark. This is a fictional demo restaurant product, not an advertisement from an existing business.

**juane**

Use case: product-mockup. Asset type: product photograph for a Peruvian delivery app promotion. Photorealistic traditional Peruvian chicken juane: rounded yellow seasoned rice parcel unwrapped in deep green bijao leaves on a simple white plate, a visible piece of cooked chicken, a half hard-boiled egg and black olive, small fresh onion salsa at the edge. Scene: clean warm ivory tabletop. Square close commercial food photograph, overhead three-quarter angle, full food visible with small margins, rice and green leaf textures clearly recognizable, natural appetizing food styling, soft daylight. No people, text, brands, logos, lettering or watermark. This is a fictional demo food photograph, not from an existing business.

**coffee**

Use case: product-mockup. Asset type: product photograph for a fictional café delivery promotion in Satipo, Peru. Photorealistic ceramic cup of freshly brewed black coffee beside a fresh chicken sandwich with tender shredded chicken and lettuce in a golden crusty small bread roll, served on a small white plate. Scene: clean warm ivory tabletop. Square close commercial food photograph, overhead three-quarter angle, entire cup and sandwich visible and centered with small margins, believable fresh food textures, soft daylight. No people, text, brands, logos, lettering or watermark. This is a fictional demo product photograph, not from an existing business.

**chaufa**

Use case: product-mockup. Square photorealistic commercial food photo of Peruvian arroz chaufa de pollo, fried rice with small tender chicken pieces, scrambled egg, green scallions and red pepper, served on a white ceramic shallow bowl on a warm ivory tabletop. Overhead three-quarter view, full dish visible, appetizing realistic texture, soft natural daylight. No people, text, brands, logos or watermarks. Fictional demo menu photograph.
