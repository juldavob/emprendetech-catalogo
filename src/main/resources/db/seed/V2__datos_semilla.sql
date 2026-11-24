-- DATOS SEMILLA (solo perfil "dev" y pruebas). Los UUID de propietarios coinciden con los
-- tokens de demostración que imprime TokensDesarrollo al arrancar.
--   Emprendedor 1: 11111111-1111-4111-8111-111111111111
--   Emprendedor 2: 22222222-2222-4222-8222-222222222222
--   Emprendedor 3: 44444444-4444-4444-8444-444444444444

INSERT INTO catalogo.tienda (id, propietario_id, nombre, descripcion, localidad, creada_en) VALUES
('a0000000-0000-4000-8000-000000000001', '11111111-1111-4111-8111-111111111111',
 'Café de la Montaña', 'Café de especialidad y productos de cacao de pequeños productores.', 'USAQUEN', now() - interval '60 days'),
('a0000000-0000-4000-8000-000000000002', '22222222-2222-4222-8222-222222222222',
 'Manos Muiscas Artesanías', 'Artesanías y tejidos hechos a mano en el centro histórico.', 'LA_CANDELARIA', now() - interval '45 days'),
('a0000000-0000-4000-8000-000000000003', '44444444-4444-4444-8444-444444444444',
 'Huerta Urbana Suba', 'Productos de huerta, apiario y cosmética natural.', 'SUBA', now() - interval '30 days');

INSERT INTO catalogo.producto
 (id, tienda_id, nombre, descripcion, categoria, precio, stock, estado, motivo_moderacion, creado_en, actualizado_en) VALUES
('b0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001',
 'Café de origen Huila 500 g', 'Café especial de tostión media con notas a panela y cacao, cultivado en Pitalito.',
 'BEBIDAS', 38000.00, 120, 'ACTIVO', NULL, now() - interval '20 days', now() - interval '20 days'),
('b0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001',
 'Café molido para filtro 250 g', 'Molienda media, ideal para cafetera de filtro o chemex.',
 'BEBIDAS', 21000.00, 200, 'ACTIVO', NULL, now() - interval '19 days', now() - interval '19 days'),
('b0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001',
 'Chocolate de mesa artesanal', 'Chocolate de mesa 100% cacao colombiano, sin conservantes.',
 'ALIMENTOS', 15000.00, 80, 'ACTIVO', NULL, now() - interval '18 days', now() - interval '18 days'),
('b0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000001',
 'Galletas de avena y panela', 'Paquete de 12 galletas horneadas en pequeños lotes.',
 'ALIMENTOS', 9500.00, 60, 'ACTIVO', NULL, now() - interval '17 days', now() - interval '17 days'),
('b0000000-0000-4000-8000-000000000005', 'a0000000-0000-4000-8000-000000000002',
 'Mochila wayuu tejida a mano', 'Mochila de hilo de algodón con diseños tradicionales, tejida por artesanas de La Guajira.',
 'ARTESANIAS', 185000.00, 12, 'ACTIVO', NULL, now() - interval '16 days', now() - interval '16 days'),
('b0000000-0000-4000-8000-000000000006', 'a0000000-0000-4000-8000-000000000002',
 'Ruana de lana virgen', 'Ruana tradicional boyacense en lana virgen, ideal para el clima de Bogotá.',
 'MODA', 160000.00, 8, 'ACTIVO', NULL, now() - interval '15 days', now() - interval '15 days'),
('b0000000-0000-4000-8000-000000000007', 'a0000000-0000-4000-8000-000000000002',
 'Vela aromática de lavanda', 'Vela de cera de soya con aroma a lavanda, 40 horas de duración.',
 'HOGAR', 22000.00, 45, 'ACTIVO', NULL, now() - interval '14 days', now() - interval '14 days'),
('b0000000-0000-4000-8000-000000000008', 'a0000000-0000-4000-8000-000000000002',
 'Taza de cerámica artesanal', 'Taza esmaltada y pintada a mano, apta para lavavajillas.',
 'HOGAR', 35000.00, 30, 'ACTIVO', NULL, now() - interval '13 days', now() - interval '13 days'),
('b0000000-0000-4000-8000-000000000009', 'a0000000-0000-4000-8000-000000000003',
 'Miel de abejas multifloral 500 g', 'Miel cruda de apiario urbano, sin pasteurizar.',
 'ALIMENTOS', 28000.00, 50, 'ACTIVO', NULL, now() - interval '12 days', now() - interval '12 days'),
('b0000000-0000-4000-8000-000000000010', 'a0000000-0000-4000-8000-000000000003',
 'Jabón artesanal de caléndula', 'Jabón en barra con aceite de oliva y caléndula, para piel sensible.',
 'BELLEZA', 12000.00, 100, 'ACTIVO', NULL, now() - interval '11 days', now() - interval '11 days'),
('b0000000-0000-4000-8000-000000000011', 'a0000000-0000-4000-8000-000000000003',
 'Queso campesino fresco 500 g', 'Queso campesino de leche de vaca, elaborado en la sabana de Bogotá.',
 'ALIMENTOS', 18000.00, 25, 'ACTIVO', NULL, now() - interval '10 days', now() - interval '10 days'),
('b0000000-0000-4000-8000-000000000012', 'a0000000-0000-4000-8000-000000000003',
 'Aromáticas deshidratadas', 'Mezcla de manzanilla y hierbabuena de la huerta, caja de 20 sobres.',
 'BEBIDAS', 8000.00, 70, 'ACTIVO', NULL, now() - interval '9 days', now() - interval '9 days'),
('b0000000-0000-4000-8000-000000000013', 'a0000000-0000-4000-8000-000000000003',
 'Cama para perro tejida en fique', 'Cama resistente y lavable, tejida en fibra natural de fique.',
 'MASCOTAS', 65000.00, 0, 'ACTIVO', NULL, now() - interval '8 days', now() - interval '8 days'),
('b0000000-0000-4000-8000-000000000014', 'a0000000-0000-4000-8000-000000000003',
 'Perfume importado sin registro', 'Perfume de procedencia no verificable.',
 'BELLEZA', 90000.00, 5, 'OCULTO', 'Producto sin registro sanitario (INVIMA).', now() - interval '7 days', now() - interval '6 days');
