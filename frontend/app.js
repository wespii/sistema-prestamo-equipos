/* Plataforma de préstamo de equipos: interfaz sin dependencias. */
const $ = (selector, root = document) => root.querySelector(selector);
const page = $('#page-content');
const modal = $('#modal');
const state = {
  token: sessionStorage.getItem('prestamos.token'),
  user: null,
  view: 'catalogo',
  categories: [],
  equipment: [],
  users: [],
  requests: [],
  loans: [],
  filters: { texto: '', categoriaId: '', estado: '' },
  requestFilters: { texto: '', estado: '' },
  loanFilters: { texto: '', estado: '' },
  loanPolicy: { maxDiasEstudiante: 7, maxDiasDocente: 30 },
  renderId: 0,
};

const I = {
  grid: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>',
  list: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 6h12M9 12h12M9 18h12M4 6h.01M4 12h.01M4 18h.01"/></svg>',
  box: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m3 7 9-4 9 4v10l-9 4-9-4V7Z"/><path d="m3 7 9 4 9-4M12 11v10"/></svg>',
  users: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M16 20v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM22 20v-2a4 4 0 0 0-3-3.87M16 2.13a4 4 0 0 1 0 7.75"/></svg>',
  chart: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20V11M10 20V4M16 20v-8M22 20v-13M2 20h22"/></svg>',
  history: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 12a9 9 0 1 0 3-6.7L3 8M3 3v5h5M12 7v5l3 2"/></svg>',
  search: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/><path d="m16 16 5 5"/></svg>',
  plus: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>',
  arrow: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14m-6-6 6 6-6 6"/></svg>',
  laptop: '<svg viewBox="0 0 100 100" aria-hidden="true"><rect x="18" y="16" width="64" height="49" rx="4"/><path d="M12 70h76l-6 10H18l-6-10ZM45 75h10"/></svg>',
  tablet: '<svg viewBox="0 0 100 100" aria-hidden="true"><rect x="29" y="10" width="42" height="78" rx="6"/><path d="M43 18h14M48 80h4"/></svg>',
  proyector: '<svg viewBox="0 0 100 100" aria-hidden="true"><rect x="13" y="32" width="74" height="39" rx="7"/><circle cx="65" cy="51" r="12"/><path d="M20 71v8M80 71v8M25 42h14"/></svg>',
  camara: '<svg viewBox="0 0 100 100" aria-hidden="true"><path d="M18 36h14l6-9h24l6 9h14a5 5 0 0 1 5 5v35a5 5 0 0 1-5 5H18a5 5 0 0 1-5-5V41a5 5 0 0 1 5-5Z"/><circle cx="50" cy="56" r="15"/></svg>',
};

const navItems = [
  { id: 'catalogo', label: 'Catálogo de equipos', icon: I.grid, roles: ['PUBLICO', 'ESTUDIANTE', 'DOCENTE', 'SOLICITANTE', 'ENCARGADO', 'ADMINISTRADOR'] },
  { id: 'mis-solicitudes', label: 'Mis solicitudes', icon: I.list, roles: ['ESTUDIANTE', 'DOCENTE', 'SOLICITANTE'] },
  { id: 'mis-prestamos', label: 'Mis préstamos', icon: I.box, roles: ['ESTUDIANTE', 'DOCENTE', 'SOLICITANTE'] },
  { id: 'solicitudes', label: 'Gestionar solicitudes', icon: I.list, roles: ['ENCARGADO', 'ADMINISTRADOR'], group: 'GESTIÓN' },
  { id: 'prestamos', label: 'Préstamos y devoluciones', icon: I.box, roles: ['ENCARGADO', 'ADMINISTRADOR'] },
  { id: 'admin-equipos', label: 'Equipos', icon: I.grid, roles: ['ADMINISTRADOR'], group: 'ADMINISTRACIÓN' },
  { id: 'admin-categorias', label: 'Categorías', icon: I.list, roles: ['ADMINISTRADOR'] },
  { id: 'admin-usuarios', label: 'Usuarios', icon: I.users, roles: ['ADMINISTRADOR'] },
  { id: 'reportes', label: 'Indicadores', icon: I.chart, roles: ['ENCARGADO', 'ADMINISTRADOR'] },
  { id: 'auditoria', label: 'Auditoría', icon: I.history, roles: ['ADMINISTRADOR'] },
];

function esc(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char]));
}
function attr(value) { return esc(value); }
function role() { return state.user?.rol || 'PUBLICO'; }
function isApplicant() { return ['ESTUDIANTE', 'DOCENTE', 'SOLICITANTE'].includes(role()); }
function isStaff() { return ['ENCARGADO', 'ADMINISTRADOR'].includes(role()); }
function isAdmin() { return role() === 'ADMINISTRADOR'; }
function canHandleLoans() { return role() === 'ENCARGADO'; }
function limitedCatalogRole() { return ['PUBLICO', 'ESTUDIANTE', 'DOCENTE', 'SOLICITANTE'].includes(role()); }
function roleName(value) { return ({ PUBLICO: 'Visitante', ESTUDIANTE: 'Estudiante', DOCENTE: 'Docente', SOLICITANTE: 'Estudiante', ENCARGADO: 'Encargado', ADMINISTRADOR: 'Administrador' })[value] || value; }
function maxLoanDays() { return role() === 'DOCENTE' ? state.loanPolicy.maxDiasDocente : state.loanPolicy.maxDiasEstudiante; }
function datePlusDays(iso, days) { const d = new Date(`${iso}T12:00:00Z`); d.setUTCDate(d.getUTCDate() + days); return d.toISOString().slice(0, 10); }
function calendarDays(start, end) { return Math.round((Date.parse(`${end}T12:00:00Z`) - Date.parse(`${start}T12:00:00Z`)) / 86400000); }
function normalized(value) { return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase(); }
function date(value) { if (!value) return '—'; const d = new Date(value.length === 10 ? `${value}T12:00:00` : value); return Number.isNaN(d.getTime()) ? esc(value) : new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: 'short', year: 'numeric' }).format(d); }
function dateTime(value) { if (!value) return '—'; const d = new Date(value); return Number.isNaN(d.getTime()) ? esc(value) : new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' }).format(d); }
function today() { const d = new Date(); d.setMinutes(d.getMinutes() - d.getTimezoneOffset()); return d.toISOString().slice(0, 10); }
function status(value) {
  const labels = { DISPONIBLE: 'Disponible', RESERVADO: 'Reservado', PRESTADO: 'Prestado', MANTENIMIENTO: 'Mantenimiento', INACTIVO: 'Inactivo', PENDIENTE: 'Pendiente', APROBADA: 'Aprobada', ENTREGADA: 'Entregada', RECHAZADA: 'Rechazada', CANCELADA: 'Cancelada', VENCIDA: 'Vencida', ACTIVO: 'Activo', DEVUELTO: 'Devuelto', DEVUELTO_CON_NOVEDAD: 'Devuelto con novedad' };
  const styles = { DISPONIBLE: 'available', RESERVADO: 'reserved', PRESTADO: 'loaned', MANTENIMIENTO: 'maintenance', INACTIVO: 'inactive', PENDIENTE: 'pending', APROBADA: 'approved', ENTREGADA: 'active', RECHAZADA: 'rejected', CANCELADA: 'inactive', VENCIDA: 'overdue', ACTIVO: 'active', DEVUELTO: 'inactive', DEVUELTO_CON_NOVEDAD: 'damaged' };
  return `<span class="badge ${styles[value] || 'inactive'}">${esc(labels[value] || value || 'Sin estado')}</span>`;
}
function equipmentKind(item) {
  const text = `${item?.nombre || ''} ${item?.categoriaNombre || ''}`.toLowerCase();
  if (/tablet|ipad/.test(text)) return 'tablet';
  if (/proyector|video beam|videobeam/.test(text)) return 'proyector';
  if (/cámara|camara|fotogr/.test(text)) return 'camara';
  return 'laptop';
}
const defaultEquipmentImages = {
  'PORT-001': '/images/equipos/port-001.png',
  'PORT-002': '/images/equipos/port-002.png',
  'PROY-001': '/images/equipos/proy-001.png',
  'PROY-002': '/images/equipos/proy-002.jpg',
  'ACCE-001': '/images/equipos/acce-001.png',
};
const referenceEquipmentImages = {
  laptop: '/images/equipos/port-001.png',
  proyector: '/images/equipos/proy-001.png',
  camara: '/images/equipos/acce-001.png',
};
function equipmentArt(item) {
  const kind = equipmentKind(item); const fallback = referenceEquipmentImages[kind];
  const image = item?.imagenUrl || defaultEquipmentImages[item?.codigo] || fallback;
  const fallbackAttr = fallback && image !== fallback ? ` data-fallback="${attr(fallback)}"` : '';
  return `<div class="equipment-image" data-kind="${kind}">${image ? `<img src="${attr(image)}"${fallbackAttr} alt="Fotografía de referencia de ${attr(item?.nombre || 'equipo tecnológico')}" loading="lazy">` : I[kind]}</div>`;
}
function availabilityText(item) {
  if (item.estado === 'DISPONIBLE') return 'Disponible ahora';
  if (item.fechaDisponibleEstimada) return `Disponible desde ${date(item.fechaDisponibleEstimada)}`;
  return 'Sin fecha de disponibilidad estimada';
}
function errorMessage(err) { return err?.message || 'No pudimos completar la operación. Inténtalo de nuevo.'; }
function toast(message, error = false) {
  const item = document.createElement('div'); item.className = `toast${error ? ' error' : ''}`; item.textContent = message;
  $('#toast-region').append(item); setTimeout(() => item.remove(), 5200);
}
function busy(button, value) { if (!button) return; button.disabled = value; if (value) { button.dataset.previousText = button.textContent; button.textContent = 'Procesando…'; } else if (button.dataset.previousText) button.textContent = button.dataset.previousText; }
function parseList(value) { return Array.isArray(value) ? value : (Array.isArray(value?.content) ? value.content : []); }
async function api(path, options = {}) {
  const isMultipart = typeof FormData !== 'undefined' && options.body instanceof FormData;
  const headers = { Accept: 'application/json', ...(options.body && !isMultipart ? { 'Content-Type': 'application/json' } : {}), ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}), ...options.headers };
  let response;
  try { response = await fetch(path, { ...options, headers }); }
  catch { throw new Error('No hay conexión con el servidor. Revisa la red e inténtalo de nuevo.'); }
  const text = await response.text(); let result = null;
  try { result = text ? JSON.parse(text) : null; } catch { result = text; }
  if (!response.ok) {
    if (response.status === 401 && !path.includes('/auth/login') && !path.includes('/auth/me') && state.token) {
      state.token = null; state.user = null; sessionStorage.removeItem('prestamos.token'); renderChrome();
      throw new Error('Tu sesión terminó. Inicia sesión de nuevo.');
    }
    const err = new Error(result?.message || result?.error || `Error ${response.status}. Inténtalo de nuevo.`); err.status = response.status; throw err;
  }
  return result;
}
async function loadLoanPolicy() {
  try {
    const policy = await api('/api/configuracion/prestamos');
    if (Number.isInteger(policy?.maxDiasEstudiante) && policy.maxDiasEstudiante > 0
        && Number.isInteger(policy?.maxDiasDocente) && policy.maxDiasDocente > 0) {
      state.loanPolicy = policy;
    }
  } catch { /* El backend vuelve a validar el plazo aunque este resumen no cargue. */ }
}
function loading() { return '<div class="loading"><span class="spinner" aria-hidden="true"></span>Cargando información…</div>'; }
function errorBox(message) { return `<div class="error-box" role="alert">${esc(message)}</div>`; }
function empty(title, description) { return `<div class="empty-state">${I.box}<strong>${esc(title)}</strong><p>${esc(description)}</p></div>`; }
function pageHead(kicker, title, description, actions = '') { return `<div class="page-head"><div><p class="eyebrow">${esc(kicker)}</p><h1>${esc(title)}</h1><p>${esc(description)}</p></div><div class="head-actions">${actions}</div></div>`; }
function table(headers, rows, title, description) {
  if (!rows.length) return empty(title, description);
  return `<div class="table-scroll"><table class="data-table"><thead><tr>${headers.map(h => `<th scope="col">${esc(h)}</th>`).join('')}</tr></thead><tbody>${rows.join('')}</tbody></table></div>`;
}
function staffFilters(kind, values, states) {
  return `<form class="filters staff-filters" id="${kind}-filter-form" aria-label="Filtrar ${kind === 'request' ? 'solicitudes' : 'préstamos'}"><div class="field"><label for="${kind}-filter-text">Usuario o equipo</label><div class="search-wrap">${I.search}<input id="${kind}-filter-text" name="texto" type="search" placeholder="Nombre, código o ID" value="${attr(values.texto)}"></div></div><div class="field"><label for="${kind}-filter-status">Estado</label><select id="${kind}-filter-status" name="estado"><option value="">Todos los estados</option>${states.map(s => `<option value="${s}" ${s === values.estado ? 'selected' : ''}>${esc(s.charAt(0)+s.slice(1).toLowerCase().replaceAll('_',' '))}</option>`).join('')}</select></div><button class="button button-primary" type="submit">Filtrar</button></form>`;
}
function renderChrome() {
  const currentRole = role(); let lastGroup = '';
  $('#main-nav').innerHTML = navItems.filter(item => item.roles.includes(currentRole)).map(item => {
    let group = ''; if (item.group && item.group !== lastGroup) { group = `<div class="nav-heading">${item.group}</div>`; lastGroup = item.group; }
    return `${group}<a class="nav-item${state.view === item.id ? ' active' : ''}" href="#${item.id}" data-view="${item.id}" ${state.view === item.id ? 'aria-current="page"' : ''}>${item.icon}<span>${item.label}</span></a>`;
  }).join('');
  const current = navItems.find(item => item.id === state.view);
  $('#breadcrumb-current').textContent = current?.label || 'Catálogo';
  $('#login-button').hidden = !!state.user; $('#user-menu').hidden = !state.user; $('#session-pill').hidden = !state.user;
  if (state.user) {
    $('#session-pill').textContent = roleName(currentRole).toUpperCase();
    $('#user-name').textContent = state.user.nombre || state.user.email;
    $('#user-role').textContent = roleName(currentRole);
    $('#user-avatar').textContent = (state.user.nombre || state.user.email || 'U').split(/\s+/).slice(0, 2).map(s => s[0]).join('').toUpperCase();
  }
}
function closeSidebar() { $('#sidebar').classList.remove('open'); $('#sidebar-scrim').hidden = true; $('#menu-toggle').setAttribute('aria-expanded', 'false'); }
function openModal(eyebrow, title, body) {
  if (modal.open) modal.close();
  $('#modal-eyebrow').textContent = eyebrow; $('#modal-title').textContent = title; $('#modal-body').innerHTML = body;
  modal.showModal();
  setTimeout(() => $('#modal-body input, #modal-body select, #modal-body button')?.focus(), 0);
}
function closeModal() { if (modal.open) modal.close(); }
function permitted(view) { return navItems.find(item => item.id === view)?.roles.includes(role()); }
async function showView(view = 'catalogo', updateHash = true) {
  view = permitted(view) ? view : 'catalogo'; state.view = view; const renderId = ++state.renderId;
  if (updateHash && location.hash !== `#${view}`) history.replaceState(null, '', `#${view}`);
  closeSidebar(); renderChrome(); page.innerHTML = loading();
  try {
    const renderers = { catalogo: renderCatalog, 'mis-solicitudes': renderMyRequests, 'mis-prestamos': renderMyLoans, solicitudes: renderRequests, prestamos: renderLoans, 'admin-equipos': renderAdminEquipment, 'admin-categorias': renderCategories, 'admin-usuarios': renderUsers, reportes: renderReports, auditoria: renderAudit };
    const html = await renderers[view](); if (renderId === state.renderId) page.innerHTML = html;
  } catch (err) { if (renderId === state.renderId) page.innerHTML = `${pageHead('PLATAFORMA', 'No se pudo cargar', 'Revisa la conexión y vuelve a intentarlo.')}${errorBox(errorMessage(err))}<p><button class="button button-secondary" data-action="retry">Reintentar</button></p>`; }
}

async function categories() { state.categories = parseList(await api('/api/categorias')); return state.categories; }
async function adminCategories() { state.categories = parseList(await api('/api/admin/categorias')); return state.categories; }
async function adminEquipment() { state.equipment = parseList(await api('/api/admin/equipos')); return state.equipment; }
async function equipment(filters = {}) {
  const params = new URLSearchParams(); Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, value); });
  const list = parseList(await api(`/api/equipos${params.size ? `?${params}` : ''}`)); state.equipment = list; return list;
}
function categoryOptions(selected = '') {
  return state.categories.filter(c => c.activo !== false || String(c.id) === String(selected)).map(c => `<option value="${attr(c.id)}" ${String(c.id) === String(selected) ? 'selected' : ''}>${esc(c.nombre)}${c.activo === false ? ' (inactiva)' : ''}</option>`).join('');
}
function equipmentCard(item) {
  return `<article class="equipment-card">${equipmentArt(item)}<div class="equipment-card-body"><div class="card-meta"><span class="code">${esc(item.codigo)}</span>${status(item.estado)}</div><h3>${esc(item.nombre)}</h3><p>${esc((item.descripcion || '').slice(0, 95))}${(item.descripcion || '').length > 95 ? '…' : ''}</p><p class="availability-note">${esc(availabilityText(item))}</p><div class="card-bottom"><span class="category">${esc(item.categoriaNombre || 'Sin categoría')}</span><button class="text-link" type="button" data-action="detail" data-id="${attr(item.id)}">Ver detalle →</button></div></div></article>`;
}
async function renderCatalog() {
  const filters = limitedCatalogRole() ? { ...state.filters, estado: '' } : state.filters;
  const [cats, items] = await Promise.all([categories(), equipment(filters)]);
  const available = items.filter(item => item.estado === 'DISPONIBLE' && item.disponible !== false).length;
  const estadoFilter = limitedCatalogRole() ? '' : `<div class="field"><label for="filter-status">Estado</label><select id="filter-status" name="estado"><option value="">Todos los estados</option>${['DISPONIBLE','RESERVADO','PRESTADO','MANTENIMIENTO','INACTIVO'].map(s => `<option value="${s}" ${s === state.filters.estado ? 'selected' : ''}>${esc(s.charAt(0)+s.slice(1).toLowerCase())}</option>`).join('')}</select></div>`;
  return `<section class="hero" aria-labelledby="hero-title"><div class="hero-copy"><p class="eyebrow">RECURSOS A TU ALCANCE</p><h1 id="hero-title">La tecnología que necesitas, cuando la necesitas.</h1><p>Explora los equipos disponibles, conoce sus características y gestiona tu solicitud desde un solo lugar.</p><button class="button" type="button" data-action="hero-cta">${state.user ? (isApplicant() ? 'Ver mis solicitudes' : 'Explorar equipos') : 'Iniciar sesión'} ${I.arrow}</button></div><div class="hero-visual" aria-hidden="true"><span></span></div></section>
    <div class="section-title"><div><h2>Explorar equipos</h2><p>Encuentra el recurso adecuado para tu actividad.</p></div><span class="count-label">${items.length} resultado${items.length === 1 ? '' : 's'} · ${available} disponible${available === 1 ? '' : 's'}</span></div>
    <form class="filters" id="filter-form" aria-label="Filtrar equipos"><div class="field"><label for="filter-text">Buscar equipo</label><div class="search-wrap">${I.search}<input id="filter-text" name="texto" type="search" placeholder="Nombre, código o descripción" value="${attr(state.filters.texto)}"></div></div><div class="field"><label for="filter-category">Categoría</label><select id="filter-category" name="categoriaId"><option value="">Todas las categorías</option>${cats.filter(c => c.activo !== false).map(c => `<option value="${attr(c.id)}" ${String(c.id) === String(state.filters.categoriaId) ? 'selected' : ''}>${esc(c.nombre)}</option>`).join('')}</select></div>${estadoFilter}<button class="button button-primary" type="submit">Filtrar</button></form>
    <div class="section-title"><h2>Catálogo</h2><span class="count-label">Selecciona un equipo para ver más información</span></div>${items.length ? `<div class="grid">${items.map(equipmentCard).join('')}</div>` : empty('No encontramos equipos', 'Prueba con otros filtros o vuelve a consultar más tarde.')}`;
}
async function detailModal(id) {
  const item = await api(`/api/equipos/${encodeURIComponent(id)}`);
  const canRequest = isApplicant() && item.estado === 'DISPONIBLE' && item.disponible !== false;
  const desc = `<div class="detail-summary">${equipmentArt(item)}<div><span class="code">${esc(item.codigo)}</span><h3>${esc(item.nombre)}</h3>${status(item.estado)}<p>${esc(item.descripcion || 'Sin descripción registrada.')}</p></div></div><div class="detail-list"><div class="detail-item"><small>Categoría</small><strong>${esc(item.categoriaNombre || 'Sin categoría')}</strong></div><div class="detail-item"><small>Disponibilidad estimada</small><strong>${esc(availabilityText(item))}</strong></div></div><div class="divider"></div><p class="form-note"><strong>Características</strong><br>${esc(item.caracteristicas || 'No hay características adicionales registradas.')}</p><div id="history-slot"></div><div class="modal-actions">${!state.user ? '<button class="button button-secondary" type="button" data-action="login">Iniciar sesión para solicitar</button>' : ''}${canRequest ? `<button class="button button-primary" type="button" data-action="request" data-id="${attr(item.id)}">Solicitar equipo ${I.arrow}</button>` : ''}</div>`;
  openModal('DETALLE DEL EQUIPO', item.nombre, desc);
  if (isStaff()) {
    try {
      const history = parseList(await api(`/api/equipos/${encodeURIComponent(id)}/historial`));
      if (modal.open && $('#history-slot')) $('#history-slot').innerHTML = `<div class="divider"></div><strong style="font-size:12px">Historial del equipo</strong>${history.length ? `<div class="timeline">${history.slice(0, 8).map(row => `<div class="timeline-item"><strong>${esc(row.accion || row.estado || row.tipo || 'Movimiento')}</strong><p>${dateTime(row.fecha || row.fechaEvento)} · ${esc(row.detalle || row.descripcion || '')}</p></div>`).join('')}</div>` : '<p class="form-note">Sin movimientos registrados.</p>'}`;
    } catch { /* El detalle principal continúa disponible. */ }
  }
}
function requestModal(id) {
  if (!state.user) { loginModal(); return; }
  if (!isApplicant()) { toast('Solo estudiantes y docentes pueden crear solicitudes.', true); return; }
  const item = state.equipment.find(e => String(e.id) === String(id));
  if (item && (item.estado !== 'DISPONIBLE' || item.disponible === false)) { toast('Este equipo no está disponible para solicitar.', true); return; }
  const days = maxLoanDays();
  openModal('NUEVA SOLICITUD', 'Solicitar equipo', `<p class="login-intro">${item ? `Vas a solicitar <strong>${esc(item.nombre)}</strong>. ` : ''}Indica cuándo lo necesitas y para qué lo usarás.</p><p class="notice" id="loan-policy-note">Perfil ${esc(roleName(role()).toLowerCase())}: préstamo de hasta <strong>${days} días calendario</strong>.</p><form id="request-form"><input type="hidden" name="equipoId" value="${attr(id)}"><div class="form-grid"><div class="field"><label for="request-start">Fecha de inicio</label><input id="request-start" name="fechaInicio" type="date" min="${today()}" required></div><div class="field"><label for="request-end">Fecha de devolución</label><input id="request-end" name="fechaFin" type="date" min="${datePlusDays(today(), 1)}" required></div><div class="field full"><label for="request-reason">Motivo de la solicitud</label><textarea id="request-reason" name="motivo" maxlength="500" placeholder="Describe brevemente la actividad para la que necesitas el equipo" required></textarea></div></div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancelar</button><button class="button button-primary" type="submit">Enviar solicitud</button></div></form>`);
  const start = $('#request-start'); const end = $('#request-end');
  start.addEventListener('change', () => {
    if (!start.value) return;
    end.min = datePlusDays(start.value, 1);
    end.max = datePlusDays(start.value, days);
    if (end.value && (end.value < end.min || end.value > end.max)) end.value = '';
  });
}

async function renderMyRequests() {
  state.requests = parseList(await api('/api/solicitudes/mias'));
  const rows = state.requests.map(s => `<tr><td><strong>${esc(s.equipoNombre || 'Equipo')}</strong><span class="sub">${esc(s.equipoCodigo || '')}</span></td><td>${date(s.fechaInicio)} – ${date(s.fechaFin)}</td><td>${dateTime(s.fechaSolicitud)}</td><td>${status(s.estado)}</td><td>${esc(s.observacionDecision || '—')}</td><td><div class="table-actions">${s.estado === 'PENDIENTE' ? `<button class="button button-danger" type="button" data-action="cancel-request" data-id="${attr(s.id)}">Cancelar</button>` : ''}</div></td></tr>`);
  return `${pageHead('MI ACTIVIDAD', 'Mis solicitudes', 'Consulta el avance de tus solicitudes y cancela las que siguen pendientes.', `<button class="button button-primary" data-view="catalogo" type="button">${I.plus} Nueva solicitud</button>`)}<div class="panel"><div class="panel-head"><h2>Historial de solicitudes</h2><span class="count-label">${state.requests.length} registro${state.requests.length === 1 ? '' : 's'}</span></div>${table(['Equipo','Periodo','Solicitada','Estado','Observación','Acciones'],rows,'Aún no tienes solicitudes','Explora el catálogo y solicita tu primer equipo.')}</div>`;
}
async function renderMyLoans() {
  state.loans = parseList(await api('/api/prestamos/mios'));
  const rows = state.loans.map(p => `<tr><td><strong>${esc(p.equipoNombre || 'Equipo')}</strong><span class="sub">Préstamo #${esc(p.id)}</span></td><td>${dateTime(p.fechaEntrega)}</td><td>${date(p.fechaDevolucionEsperada)}</td><td>${p.fechaDevolucionReal ? dateTime(p.fechaDevolucionReal) : '—'}</td><td>${p.vencido && p.estado === 'ACTIVO' ? status('VENCIDA') : status(p.estado)}</td><td>${p.novedades?.length ? esc(p.novedades.map(n => n.descripcion || n.observacion || n).join('; ')) : '—'}</td></tr>`);
  return `${pageHead('MI ACTIVIDAD', 'Mis préstamos', 'Sigue tus entregas y fechas de devolución.') }<div class="panel"><div class="panel-head"><h2>Historial de préstamos</h2><span class="count-label">${state.loans.length} registro${state.loans.length === 1 ? '' : 's'}</span></div>${table(['Equipo','Entregado','Devolución esperada','Devolución real','Estado','Novedades'],rows,'Aún no tienes préstamos','Cuando te entreguen un equipo aparecerá aquí.')}</div>`;
}
async function renderRequests() {
  state.requests = parseList(await api('/api/solicitudes'));
  const pending = state.requests.filter(s => s.estado === 'PENDIENTE').length;
  const approved = state.requests.filter(s => s.estado === 'APROBADA').length;
  const delivered = state.requests.filter(s => s.estado === 'ENTREGADA').length;
  const { texto, estado } = state.requestFilters;
  const filtered = state.requests.filter(s => (!estado || s.estado === estado) && (!texto || normalized(`${s.id} ${s.equipoCodigo} ${s.equipoNombre} ${s.solicitanteNombre}`).includes(normalized(texto))));
  const rows = filtered.map(s => `<tr><td><strong>${esc(s.equipoNombre || 'Equipo')}</strong><span class="sub">${esc(s.equipoCodigo || '')}</span></td><td>${esc(s.solicitanteNombre || '—')}</td><td>${date(s.fechaInicio)} – ${date(s.fechaFin)}</td><td><span title="${attr(s.motivo || '')}">${esc((s.motivo || '—').slice(0, 55))}${(s.motivo || '').length > 55 ? '…' : ''}</span></td><td>${status(s.estado)}</td><td><div class="table-actions">${canHandleLoans() && s.estado === 'PENDIENTE' ? `<button class="button button-primary" type="button" data-action="decide" data-id="${attr(s.id)}">Decidir</button>` : ''}${canHandleLoans() && s.estado === 'APROBADA' ? `<button class="button button-secondary" type="button" data-action="deliver" data-id="${attr(s.id)}">Entregar</button>` : ''}</div></td></tr>`);
  return `${pageHead('GESTIÓN', 'Solicitudes', canHandleLoans() ? 'Evalúa solicitudes pendientes y registra la entrega de las aprobadas.' : 'Consulta las solicitudes y su estado.') }<div class="stat-grid"><div class="stat-card alert"><div class="stat-icon">${I.list}</div><strong>${pending}</strong><span>Pendientes de decisión</span></div><div class="stat-card"><div class="stat-icon">${I.box}</div><strong>${approved}</strong><span>Aprobadas por entregar</span></div><div class="stat-card"><div class="stat-icon">${I.history}</div><strong>${delivered}</strong><span>Entregadas</span></div></div>${staffFilters('request', state.requestFilters, ['PENDIENTE','APROBADA','ENTREGADA','RECHAZADA','CANCELADA','VENCIDA'])}<div class="panel" style="margin-top:15px"><div class="panel-head"><h2>Bandeja de solicitudes</h2><span class="count-label">${filtered.length} resultado${filtered.length === 1 ? '' : 's'}</span></div>${table(['Equipo','Solicitante','Periodo','Motivo','Estado','Acciones'],rows,'No hay resultados','Prueba otros filtros o espera nuevas solicitudes.')}</div>`;
}
function decideModal(id) {
  const item = state.requests.find(s => String(s.id) === String(id)); if (!item) return;
  openModal('GESTIÓN DE SOLICITUDES', `Decidir solicitud #${id}`, `<p class="login-intro"><strong>${esc(item.solicitanteNombre)}</strong> solicita <strong>${esc(item.equipoNombre)}</strong> del ${date(item.fechaInicio)} al ${date(item.fechaFin)}.<br>Motivo: ${esc(item.motivo)}</p><form id="decision-form"><input type="hidden" name="id" value="${attr(id)}"><div class="form-grid"><div class="field full"><label for="decision-value">Decisión</label><select id="decision-value" name="aprobada" required><option value="true">Aprobar</option><option value="false">Rechazar</option></select></div><div class="field full"><label for="decision-note">Observación</label><textarea id="decision-note" name="observacion" maxlength="500" placeholder="Explica la decisión o deja una indicación para el solicitante"></textarea></div></div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancelar</button><button class="button button-primary" type="submit">Guardar decisión</button></div></form>`);
}
function deliverModal(id) {
  const item = state.requests.find(s => String(s.id) === String(id)); if (!item) return;
  openModal('ENTREGA DE EQUIPO', `Confirmar entrega #${id}`, `<p class="login-intro">Confirma que entregaste <strong>${esc(item.equipoNombre)}</strong> a <strong>${esc(item.solicitanteNombre)}</strong>. La fecha de devolución prevista es el <strong>${date(item.fechaFin)}</strong>.</p><div class="notice">Al confirmar, el equipo quedará marcado como prestado y se abrirá un préstamo activo.</div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Volver</button><button class="button button-primary" type="button" data-action="confirm-delivery" data-id="${attr(id)}">Confirmar entrega</button></div>`);
}
async function renderLoans() {
  state.loans = parseList(await api('/api/prestamos'));
  const active = state.loans.filter(p => p.estado === 'ACTIVO').length;
  const overdue = state.loans.filter(p => p.estado === 'ACTIVO' && p.vencido).length;
  const { texto, estado } = state.loanFilters;
  const filtered = state.loans.filter(p => (!estado || (estado === 'VENCIDO' ? p.estado === 'ACTIVO' && p.vencido : p.estado === estado)) && (!texto || normalized(`${p.id} ${p.equipoNombre} ${p.solicitanteNombre}`).includes(normalized(texto))));
  const rows = filtered.map(p => `<tr><td><strong>${esc(p.equipoNombre || 'Equipo')}</strong><span class="sub">Préstamo #${esc(p.id)}</span></td><td>${esc(p.solicitanteNombre || '—')}</td><td>${dateTime(p.fechaEntrega)}</td><td>${date(p.fechaDevolucionEsperada)}</td><td>${p.vencido && p.estado === 'ACTIVO' ? status('VENCIDA') : status(p.estado)}</td><td><div class="table-actions">${canHandleLoans() && p.estado === 'ACTIVO' ? `<button class="button button-secondary" type="button" data-action="return" data-id="${attr(p.id)}">Registrar devolución</button>` : ''}</div></td></tr>`);
  return `${pageHead('GESTIÓN', 'Préstamos y devoluciones', canHandleLoans() ? 'Controla equipos entregados, vencimientos y novedades al recibirlos.' : 'Consulta los préstamos, vencimientos y devoluciones.') }<div class="stat-grid"><div class="stat-card"><div class="stat-icon">${I.box}</div><strong>${active}</strong><span>Préstamos activos</span></div><div class="stat-card danger"><div class="stat-icon">${I.history}</div><strong>${overdue}</strong><span>Préstamos vencidos</span></div><div class="stat-card"><div class="stat-icon">${I.list}</div><strong>${state.loans.length}</strong><span>Préstamos registrados</span></div></div>${staffFilters('loan', state.loanFilters, ['ACTIVO','VENCIDO','DEVUELTO','DEVUELTO_CON_NOVEDAD'])}<div class="panel" style="margin-top:15px"><div class="panel-head"><h2>Registro de préstamos</h2><span class="count-label">${filtered.length} resultado${filtered.length === 1 ? '' : 's'}</span></div>${table(['Equipo','Solicitante','Entregado','Devolución prevista','Estado','Acciones'],rows,'No hay resultados','Prueba otros filtros o espera nuevas entregas.')}</div>`;
}
function returnModal(id) {
  const item = state.loans.find(p => String(p.id) === String(id)); if (!item) return;
  openModal('DEVOLUCIÓN', `Recibir ${item.equipoNombre || 'equipo'}`, `<p class="login-intro">Préstamo #${esc(id)} de <strong>${esc(item.solicitanteNombre || 'solicitante')}</strong>. Inspecciona el equipo y registra su condición.</p><form id="return-form"><input type="hidden" name="id" value="${attr(id)}"><div class="form-grid"><div class="field full"><label for="return-condition">Condición del equipo</label><select id="return-condition" name="condicion" required><option value="BUENO">Bueno · sin novedades</option><option value="NOVEDAD">Novedad menor · el equipo puede reutilizarse</option><option value="DANADO">Daño crítico · enviar a mantenimiento</option></select></div><div class="field full"><label for="return-note">Observación</label><textarea id="return-note" name="observacion" maxlength="500" placeholder="Describe cualquier novedad encontrada"></textarea></div></div><p class="form-note" style="margin-top:13px">Una novedad menor queda registrada y el equipo vuelve a estar disponible. Si el daño impide reutilizarlo, selecciona daño crítico para enviarlo a mantenimiento.</p><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancelar</button><button class="button button-primary" type="submit">Registrar devolución</button></div></form>`);
}

async function renderAdminEquipment() {
  const [cats, items] = await Promise.all([adminCategories(), adminEquipment()]);
  const rows = items.map(e => `<tr><td><strong>${esc(e.codigo)}</strong></td><td><strong>${esc(e.nombre)}</strong><span class="sub">${esc((e.descripcion || '').slice(0, 55))}</span></td><td>${esc(e.categoriaNombre || '—')}</td><td>${status(e.estado)}</td><td><div class="table-actions"><button class="button button-secondary" type="button" data-action="edit-equipment" data-id="${attr(e.id)}">Editar</button>${!['INACTIVO','RESERVADO','PRESTADO'].includes(e.estado) ? `<button class="button button-danger" type="button" data-action="deactivate-equipment" data-id="${attr(e.id)}">Desactivar</button>` : ''}</div></td></tr>`);
  return `${pageHead('ADMINISTRACIÓN', 'Equipos', 'Mantén actualizado el inventario y sus estados.', `<button class="button button-primary" type="button" data-action="new-equipment">${I.plus} Nuevo equipo</button>`)}<div class="panel"><div class="panel-head"><h2>Inventario</h2><span class="count-label">${items.length} equipo${items.length === 1 ? '' : 's'}</span></div>${table(['Código','Equipo','Categoría','Estado','Acciones'],rows,'No hay equipos registrados','Crea el primer equipo para comenzar el inventario.')}</div>`;
}
function equipmentModal(id = null) {
  const item = state.equipment.find(e => String(e.id) === String(id)) || {};
  const flowStatus = ['RESERVADO','PRESTADO'].includes(item.estado);
  const kind = equipmentKind(item); const currentImage = item.imagenUrl || defaultEquipmentImages[item.codigo];
  const previewImage = currentImage || referenceEquipmentImages[kind];
  const imageRequired = currentImage ? '' : 'required';
  const imagePreview = previewImage ? `<img id="equipment-image-preview" src="${attr(previewImage)}" alt="Foto ${currentImage ? 'actual' : 'de referencia'} de ${attr(item.nombre || 'equipo')}" loading="lazy">` : I[kind];
  openModal('ADMINISTRACIÓN', id ? 'Editar equipo' : 'Nuevo equipo', `<form id="equipment-form"><input type="hidden" name="id" value="${attr(id || '')}"><div class="form-grid"><div class="field"><label>Código único</label><div class="code-field">${id ? esc(item.codigo) : 'Se genera al guardar'}</div></div><div class="field"><label for="equipment-name">Nombre</label><input id="equipment-name" name="nombre" maxlength="120" value="${attr(item.nombre || '')}" required></div><div class="field"><label for="equipment-category">Categoría</label><select id="equipment-category" name="categoriaId" required><option value="">Seleccionar…</option>${categoryOptions(item.categoriaId)}</select></div><div class="field"><label for="equipment-state">Estado</label><select id="equipment-state" name="estado" required>${flowStatus ? `<option value="${attr(item.estado)}" selected>${item.estado === 'RESERVADO' ? 'Reservado' : 'Prestado'} · gestionado por préstamo</option>` : ''}${['DISPONIBLE','MANTENIMIENTO','INACTIVO'].map(s => `<option value="${s}" ${s === (item.estado || 'DISPONIBLE') ? 'selected' : ''}>${s.charAt(0)+s.slice(1).toLowerCase()}</option>`).join('')}</select></div><div class="field full"><label for="equipment-description">Descripción</label><textarea id="equipment-description" name="descripcion" maxlength="1000" required>${esc(item.descripcion || '')}</textarea></div><div class="field full"><label for="equipment-specs">Características</label><textarea id="equipment-specs" name="caracteristicas" maxlength="2000" placeholder="Marca, modelo, accesorios o especificaciones">${esc(item.caracteristicas || '')}</textarea></div><div class="field full"><label for="equipment-image">Foto del dispositivo</label><div class="equipment-image equipment-image-preview" data-kind="${kind}">${imagePreview}</div><input id="equipment-image" name="imagen" type="file" accept="image/jpeg,image/png,image/webp" ${imageRequired}><small class="form-note">JPG, PNG o WebP; máximo 5 MB.${imageRequired ? ' Agrega una foto para completar el registro.' : ' Selecciona otra foto solo si deseas reemplazar la actual.'}</small></div></div>${flowStatus ? '<p class="form-note" style="margin-top:13px">El estado reservado o prestado lo actualiza el flujo de préstamo. Solo puedes editar los datos del equipo.</p>' : ''}<div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancelar</button><button class="button button-primary" type="submit">Guardar equipo</button></div></form>`);
  if (flowStatus) $('#equipment-state').disabled = true;
}
async function renderCategories() {
  state.categories = parseList(await api('/api/admin/categorias'));
  const rows = state.categories.map(c => `<tr><td><strong>${esc(c.nombre)}</strong></td><td>${c.activo !== false ? status('DISPONIBLE') : status('INACTIVO')}</td><td><div class="table-actions"><button class="button button-secondary" type="button" data-action="edit-category" data-id="${attr(c.id)}">Editar</button>${c.activo !== false ? `<button class="button button-danger" type="button" data-action="deactivate-category" data-id="${attr(c.id)}">Desactivar</button>` : ''}</div></td></tr>`);
  return `${pageHead('ADMINISTRACIÓN', 'Categorías', 'Organiza los equipos para que sean fáciles de encontrar.', `<button class="button button-primary" type="button" data-action="new-category">${I.plus} Nueva categoría</button>`)}<div class="panel"><div class="panel-head"><h2>Listado de categorías</h2><span class="count-label">${state.categories.length} categoría${state.categories.length === 1 ? '' : 's'}</span></div>${table(['Nombre','Estado','Acciones'],rows,'No hay categorías','Crea una categoría antes de registrar equipos.')}</div>`;
}
function categoryModal(id = null) {
  const item = state.categories.find(c => String(c.id) === String(id)) || {};
  openModal('ADMINISTRACIÓN', id ? 'Editar categoría' : 'Nueva categoría', `<form id="category-form"><input type="hidden" name="id" value="${attr(id || '')}"><div class="form-grid"><div class="field full"><label for="category-name">Nombre</label><input id="category-name" name="nombre" maxlength="100" value="${attr(item.nombre || '')}" required></div><div class="field full"><label for="category-active">Estado</label><select id="category-active" name="activo"><option value="true" ${item.activo !== false ? 'selected' : ''}>Activa</option><option value="false" ${item.activo === false ? 'selected' : ''}>Inactiva</option></select></div></div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancelar</button><button class="button button-primary" type="submit">Guardar categoría</button></div></form>`);
}
async function renderUsers() {
  state.users = parseList(await api('/api/admin/usuarios'));
  const rows = state.users.map(u => `<tr><td><strong>${esc(u.nombre)}</strong></td><td>${esc(u.email)}</td><td>${esc(roleName(u.rol))}</td><td>${u.activo !== false ? status('DISPONIBLE') : status('INACTIVO')}</td><td><div class="table-actions"><button class="button button-secondary" type="button" data-action="edit-user" data-id="${attr(u.id)}">Editar</button></div></td></tr>`);
  return `${pageHead('ADMINISTRACIÓN', 'Usuarios', 'Gestiona accesos, roles y cuentas de la plataforma.', `<button class="button button-primary" type="button" data-action="new-user">${I.plus} Nuevo usuario</button>`)}<div class="panel"><div class="panel-head"><h2>Usuarios registrados</h2><span class="count-label">${state.users.length} usuario${state.users.length === 1 ? '' : 's'}</span></div>${table(['Nombre','Correo','Rol','Estado','Acciones'],rows,'No hay usuarios','Crea una cuenta para dar acceso a la plataforma.')}</div>`;
}
function userModal(id = null) {
  const item = state.users.find(u => String(u.id) === String(id)) || {};
  openModal('ADMINISTRACIÓN', id ? 'Editar usuario' : 'Nuevo usuario', `<form id="user-form"><input type="hidden" name="id" value="${attr(id || '')}"><div class="form-grid"><div class="field full"><label for="user-fullname">Nombre completo</label><input id="user-fullname" name="nombre" maxlength="120" value="${attr(item.nombre || '')}" required></div><div class="field full"><label for="user-email">Correo electrónico</label><input id="user-email" name="email" type="email" maxlength="160" value="${attr(item.email || '')}" required></div><div class="field"><label for="user-role-field">Rol</label><select id="user-role-field" name="rol" required>${['ESTUDIANTE','DOCENTE','ENCARGADO','ADMINISTRADOR'].map(r => `<option value="${r}" ${r === (item.rol || 'ESTUDIANTE') ? 'selected' : ''}>${roleName(r)}</option>`).join('')}</select></div><div class="field"><label for="user-active">Estado</label><select id="user-active" name="activo"><option value="true" ${item.activo !== false ? 'selected' : ''}>Activo</option><option value="false" ${item.activo === false ? 'selected' : ''}>Inactivo</option></select></div><div class="field full"><label for="user-password">Contraseña ${id ? '(dejar en blanco para conservar)' : ''}</label><input id="user-password" name="password" type="password" minlength="8" autocomplete="new-password" ${id ? '' : 'required'}></div></div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancelar</button><button class="button button-primary" type="submit">Guardar usuario</button></div></form>`);
}
function indicatorCard(value, icon, label, rows, headers, tone = '') {
  return `<details class="indicator-card"><summary class="stat-card ${tone}"><span class="stat-icon">${icon}</span><strong>${esc(value)}</strong><span>${esc(label)}</span><small>Ver cuáles ▾</small></summary><div class="indicator-dropdown">${table(headers, rows, 'Sin registros', 'No hay elementos para mostrar.')}</div></details>`;
}
async function renderReports() {
  const [report, detail] = await Promise.all([api('/api/reportes/resumen'), api('/api/reportes/detalle')]);
  const byState = report?.equiposPorEstado || {};
  const total = Object.values(byState).reduce((sum, value) => sum + Number(value || 0), 0);
  const rows = Object.entries(byState).map(([key, count]) => `<tr><td>${status(key)}</td><td><strong>${esc(count)}</strong></td></tr>`);
  const equipmentRows = (detail?.equipos || []).map(e => `<tr><td><strong>${esc(e.codigo)}</strong></td><td>${esc(e.nombre)}</td><td>${esc(e.categoriaNombre)}</td><td>${status(e.estado)}</td></tr>`);
  const requestRows = (detail?.solicitudesPendientes || []).map(s => `<tr><td><strong>${esc(s.equipoNombre)}</strong></td><td>${esc(s.solicitanteNombre)}</td><td>${date(s.fechaInicio)} – ${date(s.fechaFin)}</td></tr>`);
  const loanRows = loans => loans.map(p => `<tr><td><strong>${esc(p.solicitanteNombre)}</strong></td><td>${esc(p.equipoNombre)}</td><td>${date(p.fechaDevolucionEsperada)}</td></tr>`);
  const cards = [
    indicatorCard(total, I.grid, 'Equipos registrados', equipmentRows, ['Código','Equipo','Categoría','Estado']),
    indicatorCard(report?.solicitudesPendientes ?? 0, I.list, 'Solicitudes pendientes', requestRows, ['Equipo','Solicitante','Periodo'], 'alert'),
    indicatorCard(report?.prestamosActivos ?? 0, I.box, 'Préstamos activos', loanRows(detail?.prestamosActivos || []), ['Solicitante','Equipo','Devolución prevista']),
    indicatorCard(report?.prestamosPorVencer ?? 0, I.history, 'Vencen en 3 días', loanRows(detail?.prestamosPorVencer || []), ['Solicitante','Equipo','Fecha límite'], 'alert'),
    indicatorCard(report?.prestamosVencidos ?? 0, I.history, 'Préstamos vencidos', loanRows(detail?.prestamosVencidos || []), ['Solicitante','Equipo','Fecha límite'], 'danger'),
  ].join('');
  return `${pageHead('ANÁLISIS', 'Indicadores', 'Resumen para encargado y administrador. Abre cada indicador para ver sus registros.', `<button class="button button-secondary" type="button" data-action="retry">Actualizar</button>`)}<div class="stat-grid indicator-grid">${cards}</div><div class="panel"><div class="panel-head"><h2>Equipos por estado</h2></div>${table(['Estado','Cantidad'],rows,'Sin equipos registrados','Las cifras aparecerán al agregar equipos al inventario.')}</div>`;
}
async function renderAudit() {
  const entries = parseList(await api('/api/auditoria'));
  const rows = entries.map(a => `<tr><td>${dateTime(a.fecha)}</td><td><strong>${esc(a.usuario || 'Sistema')}</strong></td><td>${esc(a.accion || '—')}</td><td>${esc(a.entidad || '—')}</td><td>${esc(a.referencia || '—')}</td><td>${esc(a.detalle || '—')}</td></tr>`);
  return `${pageHead('TRAZABILIDAD', 'Auditoría', 'Consulta los cambios y operaciones relevantes de la plataforma.', `<button class="button button-secondary" type="button" data-action="retry">Actualizar</button>`)}<div class="panel"><div class="panel-head"><h2>Registro de actividad</h2><span class="count-label">${entries.length} evento${entries.length === 1 ? '' : 's'}</span></div>${table(['Fecha','Usuario','Acción','Entidad','Referencia','Detalle'],rows,'Aún no hay actividad','Las operaciones auditadas aparecerán aquí.')}</div>`;
}

function loginModal() {
  openModal('ACCESO A LA PLATAFORMA', 'Iniciar sesión', `<p class="login-intro">Ingresa con tu cuenta institucional para gestionar solicitudes y préstamos.</p><form id="login-form"><div class="form-grid"><div class="field full"><label for="login-email">Correo electrónico</label><input id="login-email" name="email" type="email" autocomplete="username" required></div><div class="field full"><label for="login-password">Contraseña</label><input id="login-password" name="password" type="password" autocomplete="current-password" required></div></div><div class="modal-actions"><button class="button button-secondary" type="button" data-action="close-modal">Cancelar</button><button class="button button-primary" type="submit">Ingresar ${I.arrow}</button></div></form>`);
}

document.addEventListener('error', event => {
  const image = event.target;
  if (!(image instanceof HTMLImageElement) || !image.dataset.fallback || image.dataset.fallbackApplied) return;
  image.dataset.fallbackApplied = 'true';
  image.src = image.dataset.fallback;
}, true);

document.addEventListener('click', async event => {
  const viewTarget = event.target.closest('[data-view]');
  if (viewTarget) { event.preventDefault(); await showView(viewTarget.dataset.view); return; }
  const target = event.target.closest('[data-action]'); if (!target) return;
  const { action, id } = target.dataset;
  try {
    if (action === 'login') loginModal();
    if (action === 'close-modal') closeModal();
    if (action === 'retry') await showView(state.view);
    if (action === 'hero-cta') { if (!state.user) loginModal(); else if (isApplicant()) await showView('mis-solicitudes'); else $('#filter-text')?.focus(); }
    if (action === 'detail') await detailModal(id);
    if (action === 'request') requestModal(id);
    if (action === 'decide') decideModal(id);
    if (action === 'deliver') deliverModal(id);
    if (action === 'return') returnModal(id);
    if (action === 'new-equipment') equipmentModal();
    if (action === 'edit-equipment') equipmentModal(id);
    if (action === 'new-category') categoryModal();
    if (action === 'edit-category') categoryModal(id);
    if (action === 'new-user') userModal();
    if (action === 'edit-user') userModal(id);
    if (action === 'cancel-request') {
      if (!confirm('¿Cancelar esta solicitud pendiente?')) return;
      busy(target, true); await api(`/api/solicitudes/${encodeURIComponent(id)}/cancelar`, { method: 'POST' }); toast('Solicitud cancelada.'); await showView('mis-solicitudes');
    }
    if (action === 'confirm-delivery') { busy(target, true); await api(`/api/solicitudes/${encodeURIComponent(id)}/entregar`, { method: 'POST' }); closeModal(); toast('Entrega registrada y préstamo activado.'); await showView('solicitudes'); }
    if (action === 'deactivate-category') {
      const item = state.categories.find(c => String(c.id) === String(id)); if (!item || !confirm(`¿Desactivar la categoría "${item.nombre}"?`)) return;
      busy(target, true); await api(`/api/admin/categorias/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify({ nombre: item.nombre, activo: false }) }); toast('Categoría desactivada.'); await showView('admin-categorias');
    }
    if (action === 'deactivate-equipment') {
      const item = state.equipment.find(e => String(e.id) === String(id)); if (!item || !confirm(`¿Desactivar el equipo "${item.nombre}"?`)) return;
      busy(target, true); await api(`/api/admin/equipos/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify({ codigo: item.codigo, nombre: item.nombre, descripcion: item.descripcion, caracteristicas: item.caracteristicas, categoriaId: item.categoriaId, estado: 'INACTIVO' }) }); toast('Equipo desactivado.'); await showView('admin-equipos');
    }
  } catch (err) { toast(errorMessage(err), true); busy(target, false); }
});

document.addEventListener('submit', async event => {
  const form = event.target; const formId = form.getAttribute('id'); if (!formId) return; event.preventDefault();
  const data = Object.fromEntries(new FormData(form).entries()); const submit = $('button[type="submit"]', form); busy(submit, true);
  try {
    if (formId === 'filter-form') { state.filters = { texto: data.texto.trim(), categoriaId: data.categoriaId, estado: data.estado }; await showView('catalogo'); }
    if (formId === 'request-filter-form') { state.requestFilters = { texto: data.texto.trim(), estado: data.estado }; await showView('solicitudes'); }
    if (formId === 'loan-filter-form') { state.loanFilters = { texto: data.texto.trim(), estado: data.estado }; await showView('prestamos'); }
    if (formId === 'login-form') {
      const result = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ email: data.email.trim(), password: data.password }) });
      if (!result?.token || !result?.user) throw new Error('La respuesta de acceso es incompleta.');
      state.token = result.token; state.user = result.user; sessionStorage.setItem('prestamos.token', result.token); closeModal(); toast(`Bienvenido, ${state.user.nombre}.`);
      await showView(isApplicant() ? 'catalogo' : role() === 'ENCARGADO' ? 'solicitudes' : 'reportes');
    }
    if (formId === 'request-form') {
      if (data.fechaFin <= data.fechaInicio) throw new Error('La fecha de devolución debe ser posterior al inicio.');
      if (calendarDays(data.fechaInicio, data.fechaFin) > maxLoanDays()) {
        throw new Error(`Tu perfil permite como máximo ${maxLoanDays()} días calendario de préstamo.`);
      }
      await api('/api/solicitudes', { method: 'POST', body: JSON.stringify({ equipoId: Number(data.equipoId), fechaInicio: data.fechaInicio, fechaFin: data.fechaFin, motivo: data.motivo.trim() }) });
      closeModal(); toast('Solicitud enviada. Puedes seguirla en Mis solicitudes.'); await showView('mis-solicitudes');
    }
    if (formId === 'decision-form') {
      const approved = data.aprobada === 'true'; if (!approved && !data.observacion.trim()) throw new Error('Indica el motivo del rechazo en la observación.');
      await api(`/api/solicitudes/${encodeURIComponent(data.id)}/decision`, { method: 'POST', body: JSON.stringify({ aprobada: approved, observacion: data.observacion.trim() }) });
      closeModal(); toast(approved ? 'Solicitud aprobada. Ya puedes registrar la entrega.' : 'Solicitud rechazada.'); await showView('solicitudes');
    }
    if (formId === 'return-form') {
      if (data.condicion !== 'BUENO' && !data.observacion.trim()) throw new Error('Describe la novedad o daño del equipo.');
      await api(`/api/prestamos/${encodeURIComponent(data.id)}/devolver`, { method: 'POST', body: JSON.stringify({ condicion: data.condicion, observacion: data.observacion.trim() }) });
      closeModal(); toast('Devolución registrada.'); await showView('prestamos');
    }
    if (formId === 'equipment-form') {
      const creating = !data.id;
      const existing = state.equipment.find(e => String(e.id) === String(data.id));
      const payload = { codigo: existing?.codigo || null, nombre: data.nombre.trim(), categoriaId: Number(data.categoriaId), estado: data.estado || existing?.estado || 'DISPONIBLE', descripcion: data.descripcion.trim(), caracteristicas: data.caracteristicas.trim() };
      const saved = await api(data.id ? `/api/admin/equipos/${encodeURIComponent(data.id)}` : '/api/admin/equipos', { method: data.id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
      if (!data.id && saved?.id) {
        data.id = saved.id;
        $('#equipment-form input[name="id"]').value = saved.id;
        state.equipment.push(saved);
      }
      if (data.imagen instanceof File && data.imagen.size > 0) {
        const imageBody = new FormData(); imageBody.append('archivo', data.imagen);
        await api(`/api/admin/equipos/${encodeURIComponent(saved.id)}/imagen`, { method: 'PUT', body: imageBody });
      }
      closeModal(); toast(creating ? 'Equipo creado.' : 'Equipo actualizado.'); await showView('admin-equipos');
    }
    if (formId === 'category-form') {
      await api(data.id ? `/api/admin/categorias/${encodeURIComponent(data.id)}` : '/api/admin/categorias', { method: data.id ? 'PUT' : 'POST', body: JSON.stringify({ nombre: data.nombre.trim(), activo: data.activo === 'true' }) });
      closeModal(); toast(data.id ? 'Categoría actualizada.' : 'Categoría creada.'); await showView('admin-categorias');
    }
    if (formId === 'user-form') {
      const payload = { nombre: data.nombre.trim(), email: data.email.trim(), rol: data.rol, activo: data.activo === 'true' };
      if (data.password) payload.password = data.password;
      await api(data.id ? `/api/admin/usuarios/${encodeURIComponent(data.id)}` : '/api/admin/usuarios', { method: data.id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
      closeModal(); toast(data.id ? 'Usuario actualizado.' : 'Usuario creado.'); await showView('admin-usuarios');
    }
  } catch (err) { toast(errorMessage(err), true); busy(submit, false); }
});

document.addEventListener('change', event => {
  if (event.target.id !== 'equipment-image') return;
  const file = event.target.files?.[0]; const preview = $('#equipment-image-preview');
  if (!file || !preview) return;
  const objectUrl = URL.createObjectURL(file);
  if (preview.tagName === 'IMG') preview.src = objectUrl;
  else {
    const image = document.createElement('img'); image.id = 'equipment-image-preview'; image.alt = 'Vista previa de la foto seleccionada'; image.src = objectUrl;
    preview.replaceWith(image);
  }
});

$('#login-button').addEventListener('click', loginModal);
$('#logout-button').addEventListener('click', () => { sessionStorage.removeItem('prestamos.token'); state.token = null; state.user = null; closeModal(); toast('Sesión cerrada.'); showView('catalogo'); });
$('#modal-close').addEventListener('click', closeModal);
modal.addEventListener('click', event => { if (event.target === modal) closeModal(); });
$('#menu-toggle').addEventListener('click', () => { const isOpen = $('#sidebar').classList.toggle('open'); $('#sidebar-scrim').hidden = !isOpen; $('#menu-toggle').setAttribute('aria-expanded', String(isOpen)); });
$('#sidebar-scrim').addEventListener('click', closeSidebar);
window.addEventListener('hashchange', () => showView(location.hash.slice(1) || 'catalogo', false));

(async function start() {
  if (state.token) {
    try { state.user = await api('/api/auth/me'); }
    catch { sessionStorage.removeItem('prestamos.token'); state.token = null; state.user = null; }
  }
  await loadLoanPolicy();
  await showView(location.hash.slice(1) || 'catalogo');
})();
