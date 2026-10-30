'use strict';
const $ = (q, root = document) => root.querySelector(q);
const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const state = { user:null, token:null, expires:0, lifetime:300, page:'overview', vault:[], sessions:[], activity:[], users:[], csrf:null };
let rotation = null, secretTimeout, toastTimeout, renderId = 0;
const titles = {
 overview:['YOUR SECURITY AT A GLANCE','Workspace overview','A clear view of your credentials, access, and active sessions.'],
 vault:['SENSITIVE DATA, SAFELY STORED','Credential vault','Your OAuth tokens and API credentials, encrypted before they reach the database.'],
 sessions:['A SHORT LIFETIME. A STRONGER SESSION.','Tokens & sessions','Inspect your access token, rotate credentials, and revoke refresh sessions.'],
 access:['PERMISSIONS YOU CAN PROVE','Access lab','Call real protected endpoints and see role-based authorization in action.'],
 activity:['A RECORD OF WHAT MATTERS','Activity log','Trace authentication, credential access, and token lifecycle events.'],
 accounts:['ADMINISTRATION','Workspace accounts','Create accounts and assign permissions. Passwords are stored as BCrypt hashes.']
};
async function getCsrf() {
 const response = await fetch('/api/csrf',{credentials:'same-origin',cache:'no-store'});
 if(!response.ok) throw new Error('Cannot connect to Spring Boot. Start the server and open http://localhost:8080.');
 state.csrf = await response.json();
}
async function request(path,{method='GET',body,auth=true,retry=true}={}) {
 if(!state.csrf) await getCsrf();
 const headers = {};
 if(auth && state.token) headers.Authorization = 'Bearer '+state.token;
 if(method !== 'GET') headers[state.csrf.headerName] = state.csrf.token;
 if(body !== undefined) headers['Content-Type']='application/json';
 const res = await fetch('/api'+path,{method,headers,body:body===undefined?undefined:JSON.stringify(body),credentials:'same-origin',cache:'no-store'});
 let data; try { data=await res.json(); } catch { data={message:'The server returned an unexpected response.'}; }
 if(res.status===401 && auth && retry && state.user) {
  try { await rotate(); } catch(e) { signedOut(); throw e; }
  return request(path,{method,body,auth,retry:false});
 }
 if(!res.ok) { const e=new Error(data.message || 'Request failed'); e.status=res.status; e.data=data; throw e; }
 return data;
}
function applyAuth(data) {
 state.user=data.user; state.token=data.accessToken; state.lifetime=data.expiresIn;
 state.expires=Date.now()+data.expiresIn*1000;
}
async function rotate() {
 if(!rotation) rotation=request('/auth/refresh',{method:'POST',auth:false,retry:false}).then(applyAuth).finally(()=>{rotation=null;});
 return rotation;
}
function toast(message,fail=false) { clearTimeout(toastTimeout);const el=$('#toast');el.textContent=message;el.className='toast'+(fail?' failure':'');toastTimeout=setTimeout(()=>el.classList.add('hidden'),5500); }
function signedOut() {
 state.user=null; state.token=null; state.expires=0; state.vault=[];state.activity=[];state.sessions=[];state.users=[];
 closeModal(); $('#app').classList.add('hidden');$('#login-screen').classList.remove('hidden'); $('#view').replaceChildren();renderId++;
}
async function signedIn() {
 $('#login-screen').classList.add('hidden');$('#app').classList.remove('hidden');
 $('#profile-name').textContent=state.user.name;$('#profile-role').textContent=state.user.email;
 $('#avatar').textContent=state.user.name.split(/\s+/).map(x=>x[0]).slice(0,2).join('').toUpperCase();
 $('#role-badge').textContent=state.user.role+' WORKSPACE';
 document.querySelectorAll('.admin-only').forEach(el=>el.classList.toggle('hidden',state.user.role!=='ADMIN'));
 await navigate('overview');
}
const time = n => new Date(n).toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'});
const date = n => new Date(n).toLocaleDateString([],{day:'2-digit',month:'short',year:'numeric'});
const human = value => value.toLowerCase().replaceAll('_',' ').replace(/^./,c=>c.toUpperCase());
function panel(title,subtitle,body,action='') { return `<section class="panel"><div class="panel-heading"><div><h3>${title}</h3>${subtitle?`<p>${subtitle}</p>`:''}</div>${action}</div>${body}</section>`; }
function empty(title,description,button='') {return `<div class="empty"><div class="empty-symbol">◇</div><h3>${title}</h3><p>${description}</p>${button}</div>`;}
function vaultTable(items,full=false) {
 if(!items.length)return empty('Your vault is ready','Add your first sample credential to see encryption in action.','<button class="button secondary" data-action="add">＋ Add credential</button>');
 return `<div class="table-wrap"><table><thead><tr><th>Credential</th><th>Protection</th>${full?'<th>Added</th><th>Actions</th>':'<th></th>'}</tr></thead><tbody>${items.map(v=>`<tr><td><div class="provider"><span class="provider-icon">${esc(v.provider.slice(0,2).toUpperCase())}</span><div>${esc(v.provider)}<small>${esc(v.label)}</small></div></div></td><td><span class="pill"><span>✓</span> Encrypted</span></td>${full?`<td>${date(v.createdAt)}</td><td><div class="table-actions"><button class="button secondary" data-action="reveal" data-id="${esc(v.id)}">Reveal</button><button class="button secondary" data-action="cipher" data-id="${esc(v.id)}">Inspect</button><button class="button danger" data-action="delete" data-id="${esc(v.id)}">Delete</button></div></td>`:`<td><button class="link-button" data-action="reveal" data-id="${esc(v.id)}">View ↗</button></td>`}</tr>`).join('')}</tbody></table></div>`;
}
function auditRows(items) {
 if(!items.length) return empty('No events yet','Your next security action will appear here.');
 return items.map(a=>`<div class="audit-row"><span class="check">${a.action.includes('REJECTED')?'!':'✓'}</span><div><strong>${esc(human(a.action))}</strong><p>${esc(a.detail)}</p></div><time datetime="${new Date(a.createdAt).toISOString()}">${time(a.createdAt)}</time></div>`).join('');
}
function tokenCard() { return `<section class="token-card"><div class="token-top"><span class="eyebrow">ACCESS TOKEN LIFETIME</span><span>◷</span></div><div class="token-time" data-countdown>05:00</div><p>Short-lived access. Less time exposed.</p><progress data-token-progress max="${state.lifetime}" value="${state.lifetime}" aria-label="Access token lifetime remaining"></progress><button class="button" data-action="rotate">↻ Rotate tokens now</button></section>`; }
function overview() {
 const stats=[['Credentials stored',state.vault.length,'Encrypted in your personal vault','▣'],['Active sessions',state.sessions.length,'Refresh sessions that can renew access','◷'],['Your access level',state.user.role,'Enforced by Spring Security','♙'],['Vault encryption','AES-256','GCM authenticated encryption','◇']];
 return `<div class="stats">${stats.map(([l,v,c,i],n)=>`<div class="stat"><span class="stat-label">${l}</span><span class="stat-icon">${i}</span><div class="stat-value ${n>1?'text':''}">${v}</div><div class="stat-caption">${c}</div></div>`).join('')}</div><div class="dashboard-grid"><div class="stack">${panel('Your credentials <span class="count">'+state.vault.length+'</span>','Private to your account. Protected at rest.',vaultTable(state.vault.slice(0,4)),'<button class="link-button" data-nav="vault">Open vault ↗</button>')}${panel('Recent activity','Your latest security events',auditRows(state.activity.slice(0,4)),'<button class="link-button" data-nav="activity">View all ↗</button>')}</div><div class="stack">${panel('Protection layers','Working together behind every request',`<div class="panel-body">${[['JWT authentication','Signed access tokens, validated on every API call.'],['Role-based authorization','USER and ADMIN permissions, checked on the server.'],['AES-256-GCM encryption','Random nonces and authenticated ciphertext.'],['Refresh token rotation','One-time refresh tokens, with replay detection.']].map(([t,d])=>`<div class="security-item"><span class="check">✓</span><div><strong>${t}</strong><p>${d}</p></div></div>`).join('')}</div>`)}${tokenCard()}</div></div>`;
}
function vaultPage(){return `<div class="toolbar"><input id="vault-search" type="search" placeholder="Search provider or credential label…" aria-label="Search credentials"><span>${state.vault.length} credential${state.vault.length===1?'':'s'} in your vault</span></div><div class="info-strip"><span>◇</span><p>Only you can reveal your credentials. Even an administrator cannot read another user's vault. Use <strong>Inspect</strong> to see the actual encrypted database value.</p></div>${panel('All credentials','AES-256-GCM · A unique nonce for every save','<div id="vault-rows">'+vaultTable(state.vault,true)+'</div>')}`;}
function sessionsPage(){
 let claims={};try{claims=JSON.parse(atob(state.token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/')));}catch{}
 const display={issuer:claims.iss,subject:claims.sub,role:claims.role,token_use:claims.token_use,expires:new Date(claims.exp*1000).toISOString()};
 return `<div class="info-strip"><span>◷</span><p>Access tokens live for ${state.lifetime} seconds. Refresh sessions last up to 7 days by default. Revoking a session prevents renewal; already-issued JWTs remain valid until expiry.</p></div><div class="columns"><div class="stack">${tokenCard()}${panel('Current JWT claims','Decoded for display; the server verifies the signature.',`<div class="panel-body"><pre class="code">${esc(JSON.stringify(display,null,2))}</pre><p class="form-note">The access token stays in memory. The refresh token is in an HttpOnly cookie and cannot be read by this dashboard.</p></div>`)}</div><div class="stack">${panel('Active refresh sessions',state.sessions.length+' sessions can request new access tokens',state.sessions.length?state.sessions.map((s,i)=>`<div class="session-row"><span class="avatar">${String(i+1).padStart(2,'0')}</span><div><strong>Session ${esc(s.id.slice(0,8))}</strong><p>Latest token: ${date(s.createdAt)} · ${time(s.createdAt)}</p><p>Expires ${date(s.expiresAt)} · ${time(s.expiresAt)}</p></div><span class="pill">Active</span></div>`).join(''):empty('No active refresh sessions','Your access token will work until it expires.'),'<button class="link-button" data-action="revoke">Revoke all</button>')}${panel('How rotation works','Follow the lifecycle',`<div class="panel-body">${[['Sign in','Password verification issues an access JWT and a refresh cookie.'],['Request access','The JWT filter verifies the signature, issuer, audience and expiry.'],['Refresh once','A valid refresh token is consumed and replaced atomically.'],['Reject reuse','Replaying a consumed token revokes the entire refresh family.']].map(([t,d],i)=>`<div class="flow-step"><span class="step-number">${i+1}</span><div><h4>${t}</h4><p>${d}</p></div></div>`).join('')}</div>`)}</div></div>`;
}
function accessPage(){return `<div class="info-strip"><span>⇄</span><p>You are signed in as <strong>${esc(state.user.role)}</strong>. These tests call the live backend. Sign in with the other classroom account to compare results.</p></div><div class="columns">${panel('Try a protected endpoint','Responses come directly from Spring Security.',[['/test/user','USER + ADMIN','A shared endpoint available to both roles.'],['/test/admin','ADMIN ONLY','Method-level authorization using @PreAuthorize.'],['/me','NO TOKEN','Request a protected endpoint without authentication.']].map(([path,role,desc],i)=>`<div class="endpoint"><div class="endpoint-line"><code><small>GET</small>/api${path}</code><button class="button secondary" data-action="test" data-test="${i}">Send request ↗</button></div><p>${role} · ${desc}</p><div id="test-${i}" class="result"></div></div>`).join(''))}${panel('Permission matrix','Authorization is enforced on the backend.',`<div class="table-wrap"><table class="matrix"><thead><tr><th>Capability</th><th>USER</th><th>ADMIN</th></tr></thead><tbody>${[['Own credential vault',true,true],['Refresh own session',true,true],['Shared API endpoint',true,true],['Admin API endpoint',false,true],['Create accounts',false,true],['Global audit log',false,true],['Another user’s vault',false,false]].map(([label,u,a])=>`<tr><td>${label}</td><td><span class="pill ${u?'':'deny'}">${u?'Allowed':'Denied'}</span></td><td><span class="pill ${a?'':'deny'}">${a?'Allowed':'Denied'}</span></td></tr>`).join('')}</tbody></table></div>`)}</div>`;}
function activityPage(){return panel(state.user.role==='ADMIN'?'Workspace audit log':'Your activity log',state.user.role==='ADMIN'?'Latest 100 events across the workspace. No secrets are logged.':'Latest 100 events for your account.',state.activity.length?`<div class="table-wrap"><table><thead><tr><th>Event</th><th>Actor</th><th>Detail</th><th>Time</th></tr></thead><tbody>${state.activity.map(a=>`<tr><td><span class="pill ${a.action.includes('REJECTED')?'deny':''}">${esc(human(a.action))}</span></td><td>${esc(a.actor)}</td><td>${esc(a.detail)}</td><td>${time(a.createdAt)}<small>${date(a.createdAt)}</small></td></tr>`).join('')}</tbody></table></div>`:empty('No events yet','Events will appear as you use the workspace.'),'<button class="link-button" data-action="reload">↻ Refresh</button>');}
function accountsPage(){return panel('Workspace accounts','Administrator-controlled account creation',`<div class="table-wrap"><table><thead><tr><th>Account</th><th>Email address</th><th>Role</th><th>Joined</th></tr></thead><tbody>${state.users.map(u=>`<tr><td>${esc(u.name)}</td><td>${esc(u.email)}</td><td><span class="badge ${u.role==='USER'?'gray':''}">${esc(u.role)}</span></td><td>${date(u.createdAt)}</td></tr>`).join('')}</tbody></table></div>`,'<button class="button primary" data-action="new-user">＋ Create account</button>');}
async function navigate(page) {
 if(!state.user)return;if(page==='accounts'&&state.user.role!=='ADMIN')return;
 state.page=page;const ticket=++renderId;const [eyebrow,title,subtitle]=titles[page];
 $('#page-eyebrow').textContent=eyebrow;$('#page-title').textContent=title;$('#page-subtitle').textContent=subtitle;$('#breadcrumb-page').textContent=title;
 $('#add-credential').classList.toggle('hidden',!['overview','vault'].includes(page));
 document.querySelectorAll('[data-page]').forEach(el=>el.classList.toggle('active',el.dataset.page===page));
 $('#view').innerHTML='<div class="loading">Loading your workspace…</div>';
 try{
  if(page==='overview') [state.vault,state.sessions,state.activity]=await Promise.all([request('/vault'),request('/sessions'),request('/activity')]);
  if(page==='vault')state.vault=await request('/vault');
  if(page==='sessions')state.sessions=await request('/sessions');
  if(page==='activity')state.activity=await request(state.user.role==='ADMIN'?'/admin/audit':'/activity');
  if(page==='accounts')state.users=await request('/admin/users');
  if(ticket!==renderId || !state.user)return;
  $('#view').innerHTML=({overview,vault:vaultPage,sessions:sessionsPage,access:accessPage,activity:activityPage,accounts:accountsPage}[page])();tick();
 }catch(e){if(ticket===renderId)$('#view').innerHTML=empty('Could not load this view',esc(e.message),'<button class="button secondary" data-action="reload">Try again</button>');}
}
function modal(title,body,eyebrow='SECUREVAULT') {
 clearTimeout(secretTimeout);$('#modal-title').textContent=title;$('#modal-eyebrow').textContent=eyebrow;$('#modal-body').innerHTML=body;
 if(!$('#modal').open)$('#modal').showModal();
}
function closeModal(){clearTimeout(secretTimeout);$('#modal').close();$('#modal-body').replaceChildren();}
function addModal(){modal('Add a credential',`<p class="form-note">Store an OAuth token, API key, or client secret. The server encrypts the value before writing it to the database.</p><form id="credential-form"><label>Provider<input name="provider" placeholder="e.g. GitHub" required maxlength="80"></label><label>Credential label<input name="label" placeholder="e.g. Class project token" required maxlength="100"></label><label>Secret value<input name="secret" type="password" placeholder="Paste a sample token or secret" required maxlength="2000" autocomplete="off"></label><p class="error" id="form-error" role="alert"></p><div class="modal-actions"><button type="button" class="button secondary" data-action="close">Cancel</button><button type="submit" class="button primary">Encrypt & save ↗</button></div></form>`,'AES-256-GCM ENCRYPTION');}
function userModal(){modal('Create an account',`<form id="user-form"><label>Full name<input name="name" required maxlength="80" placeholder="e.g. Classmate"></label><label>Email address<input name="email" type="email" required maxlength="160" placeholder="name@example.com"></label><label>Password<input name="password" type="password" minlength="10" maxlength="72" required autocomplete="new-password" placeholder="At least 10 characters"></label><label>Role<select name="role"><option value="USER">USER — personal vault and sessions</option><option value="ADMIN">ADMIN — account management and audit</option></select></label><p class="error" id="form-error" role="alert"></p><div class="modal-actions"><button type="button" class="button secondary" data-action="close">Cancel</button><button type="submit" class="button primary">Create account</button></div></form>`,'ADMINISTRATION');}
async function action(button){
 const {action:type,id}=button.dataset;
 if(type==='add')return addModal();if(type==='close')return closeModal();if(type==='new-user')return userModal();if(type==='reload')return navigate(state.page);
 button.disabled=true;
 try{
  if(type==='rotate'){await rotate();toast('Tokens rotated. Your previous refresh token is now invalid.');await navigate(state.page);}
  if(type==='reveal'){const data=await request('/vault/'+encodeURIComponent(id)+'/reveal',{method:'POST'});modal('Credential revealed',`<p class="form-note">Decrypted for its owner. This value will clear automatically in 20 seconds.</p><div class="secret-value">${esc(data.secret)}</div><div class="modal-actions"><button class="button primary" data-action="close">Hide credential</button></div>`,'OWNER-ONLY ACCESS');secretTimeout=setTimeout(closeModal,20000);}
  if(type==='cipher'){const data=await request('/vault/'+encodeURIComponent(id)+'/ciphertext');modal('Encrypted at rest',`<p class="form-note">This is the encrypted value read from the database. The plaintext is never stored.</p><pre class="code">${esc(data.ciphertext)}</pre><p class="form-note">${esc(data.format)}</p>`,'AES-256-GCM');}
  if(type==='delete'){modal('Delete this credential?',`<p class="form-note">The encrypted record will be permanently removed from your vault.</p><div class="modal-actions"><button class="button secondary" data-action="close">Keep credential</button><button class="button danger" data-action="confirm-delete" data-id="${esc(id)}">Delete credential</button></div>`);}
  if(type==='confirm-delete'){await request('/vault/'+encodeURIComponent(id),{method:'DELETE'});closeModal();toast('Credential deleted.');await navigate(state.page);}
  if(type==='revoke'){modal('Revoke all refresh sessions?',`<p class="form-note">Every refresh session for your account will stop working. Existing access tokens remain valid until expiry. You will need to sign in again to renew access.</p><div class="modal-actions"><button class="button secondary" data-action="close">Cancel</button><button class="button danger" data-action="confirm-revoke">Revoke all</button></div>`);}
  if(type==='confirm-revoke'){const result=await request('/sessions/revoke-all',{method:'POST'});closeModal();toast(result.message);await navigate(state.page);}
  if(type==='test'){
   const i=Number(button.dataset.test),paths=['/test/user','/test/admin','/me']; const target=$('#test-'+i);
   try{const data=await request(paths[i],{auth:i!==2,retry:false});target.innerHTML='<span class="pill">200 OK · Access granted</span><pre class="code">'+esc(JSON.stringify(data,null,2))+'</pre>';}
   catch(e){target.innerHTML=`<span class="pill deny">${esc(e.status||'Network error')} · ${e.status===403?'Forbidden':e.status===401?'Unauthorized':'Request failed'}</span><pre class="code">${esc(JSON.stringify(e.data||{message:e.message},null,2))}</pre>`;}
  }
 }catch(e){toast(e.message,true);if(type==='rotate'&&e.status===401)signedOut();}finally{button.disabled=false;}
}
document.addEventListener('click',e=>{
 const demo=e.target.closest('[data-demo]');if(demo){const admin=demo.dataset.demo==='admin';$('#login-form [name=email]').value=(admin?'admin':'user')+'@securevault.local';$('#login-form [name=password]').value=admin?'Admin@12345':'User@12345';$('#login-error').textContent='';return;}
 const nav=e.target.closest('[data-page],[data-nav]');if(nav){navigate(nav.dataset.page||nav.dataset.nav);return;}
 const button=e.target.closest('[data-action]');if(button)action(button);
});
$('#add-credential').addEventListener('click',addModal);$('#close-modal').addEventListener('click',closeModal);
$('#modal').addEventListener('close',()=>{clearTimeout(secretTimeout);$('#modal-body').replaceChildren();});
$('#modal').addEventListener('click',e=>{if(e.target===$('#modal')){const r=e.target.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)closeModal();}});
document.addEventListener('input',e=>{if(e.target.id==='vault-search'){const q=e.target.value.toLowerCase();const items=state.vault.filter(v=>(v.provider+' '+v.label).toLowerCase().includes(q));$('#vault-rows').innerHTML=items.length?vaultTable(items,true):empty('No matching credentials','Try another provider or label.');}});
document.addEventListener('submit',async e=>{
 const form=e.target;if(!['login-form','credential-form','user-form'].includes(form.id))return;e.preventDefault();
 const button=form.querySelector('[type=submit]'),label=button.textContent;button.disabled=true;button.textContent='Please wait…';
 const error=form.id==='login-form'?$('#login-error'):$('#form-error');error.textContent='';
 try{
  const body=Object.fromEntries(new FormData(form));
  if(form.id==='login-form'){await getCsrf();applyAuth(await request('/auth/login',{method:'POST',body,auth:false}));form.reset();await signedIn();}
  else{await request(form.id==='credential-form'?'/vault':'/admin/users',{method:'POST',body});closeModal();toast(form.id==='credential-form'?'Credential encrypted and saved.':'Account created.');await navigate(state.page);}
 }catch(e){error.textContent=e.message;}finally{button.disabled=false;button.textContent=label;}
});
$('#logout').addEventListener('click', async () => {
 const btn = $('#logout');
 btn.disabled = true;
 btn.querySelector('.logout-label').textContent = 'Signing out…';
 try {
  await request('/auth/logout', { method: 'POST', auth: false });
  signedOut();
  toast('Signed out successfully. Your session has been revoked.');
 } catch (e) {
  // Even if the server call fails, clear local state for security
  signedOut();
  toast('Signed out. (Session cleanup: ' + e.message + ')', true);
 } finally {
  btn.disabled = false;
  btn.querySelector('.logout-label').textContent = 'Sign out';
 }
});
function tick(){if(!state.user)return;const seconds=Math.max(0,Math.ceil((state.expires-Date.now())/1000));document.querySelectorAll('[data-countdown]').forEach(el=>el.textContent=String(Math.floor(seconds/60)).padStart(2,'0')+':'+String(seconds%60).padStart(2,'0'));document.querySelectorAll('[data-token-progress]').forEach(el=>{el.max=state.lifetime;el.value=seconds;});}
setInterval(tick,1000);
(async()=>{try{await getCsrf();await rotate();await signedIn();}catch(e){if(e.status!==401)$('#login-error').textContent=e.message;}})();
