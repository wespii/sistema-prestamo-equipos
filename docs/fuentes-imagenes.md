# Fuentes de fotografías del catálogo

Las imágenes locales están en `frontend/images/equipos/`. El catálogo usa la foto asociada al código inicial y, si llega un código autogenerado sin fotografía, asigna una referencia según el tipo o categoría detectada (portátil, proyector o cámara web). Así los equipos de prueba y los nuevos equipos de esa familia también muestran una foto. Una fotografía cargada por el administrador tiene prioridad sobre estas referencias. Los datos del inventario solo identifican la marca y el tipo, no siempre el modelo exacto; las fotos muestran modelos reales de referencia de la misma familia y no afirman que sean las unidades físicas exactas de la institución. El administrador puede reemplazarlas desde **Equipos → Editar → Foto del dispositivo**.

| Código | Equipo del catálogo | Modelo de referencia | Archivo local | Fuente del fabricante |
| --- | --- | --- | --- | --- |
| `PORT-001` | Portátil Lenovo ThinkPad | ThinkPad T14 Gen 6 | `port-001.png` | [Página oficial Lenovo](https://www.lenovo.com/us/en/p/laptops/thinkpad/thinkpadt/thinkpad-t14-gen-6-14-inch-intel/len101t0127) |
| `PORT-002` | Portátil HP ProBook | ProBook 450 G10 | `port-002.png` | [Página de soporte oficial HP](https://support.hp.com/us-en/product/setup-user-guides/hp-probook-450-15.6-inch-g10-notebook-pc/model/2101593814) |
| `PROY-001` | Proyector Epson | PowerLite 992F | `proy-001.png` | [Página oficial Epson](https://epson.com/For-Work/Projectors/Classroom/PowerLite-992F-Full-HD-1080p-Classroom-Projector-with-Built-in-Wireless/p/V11H988020) |
| `PROY-002` | Proyector ViewSonic | PA503W | `proy-002.jpg` | [Página oficial ViewSonic](https://www.viewsonic.com/us/pa503w-4000-lumens-wxga-home-with-hdmi-and-vertical-keystone.html) |
| `ACCE-001` | Cámara web Logitech | C920e Business Webcam | `acce-001.png` | [Página oficial Logitech](https://www.logitech.com/en-us/products/webcams/c920e-business-webcam.960-001401.html) |

Las imágenes locales permiten que el catálogo de demostración cargue sin depender de los sitios externos. Los archivos originales fueron obtenidos de las páginas oficiales enlazadas.
