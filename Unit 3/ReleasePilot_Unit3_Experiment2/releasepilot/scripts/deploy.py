#!/usr/bin/env python3
"""Run on the dedicated deployment runner. Persist state outside its checkout."""
import argparse,json,pathlib,os,re,secrets,shutil,subprocess,urllib.request,time
p=argparse.ArgumentParser();p.add_argument('image');p.add_argument('--directory',default=str(pathlib.Path.home()/'releasepilot-deployment'));a=p.parse_args()
if not re.fullmatch(r'ghcr\.io/[a-z0-9_.\-/]+@sha256:[a-f0-9]{64}',a.image):raise SystemExit('Expected an immutable ghcr.io image digest')
source=pathlib.Path(__file__).resolve().parent.parent;dest=pathlib.Path(a.directory).resolve();dest.mkdir(parents=True,exist_ok=True)
statefile=dest/'deployment.json'
state=json.loads(statefile.read_text()) if statefile.exists() else {'promotedImage':a.image,'password':secrets.token_urlsafe(32)}
shutil.copy2(source/'compose.yaml',dest/'compose.yaml')
def env(candidate):return {**os.environ,'STABLE_IMAGE':state['promotedImage'],'CANARY_IMAGE':candidate,'GATEWAY_IMAGE':a.image,'DATABASE_PASSWORD':state['password'],'CANARY_FAIL_QUOTES':'false','STABLE_VERSION':state['promotedImage'][-12:],'CANARY_VERSION':candidate[-12:]}
def compose(candidate,*args):subprocess.run(['docker','compose',*args],cwd=dest,env=env(candidate),check=True)
def api(path,body=None):
 req=urllib.request.Request('http://localhost:8082'+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json','X-Lab-Request':'releasepilot'})
 with urllib.request.urlopen(req,timeout=10) as response:return json.load(response)
def persist():
 temporary=statefile.with_suffix('.tmp');temporary.write_text(json.dumps(state));temporary.chmod(0o600);temporary.replace(statefile)
persist()
# Restore last promoted image into both slots before replacing the candidate; this is safe even if traffic was 100% canary.
compose(state['promotedImage'],'pull');compose(state['promotedImage'],'up','-d','--wait','--wait-timeout','180');api('/api/rollout',{'percent':0})
try:
 compose(a.image,'pull','canary');compose(a.image,'up','-d','--wait','--wait-timeout','180','canary')
 subprocess.run(['python3',str(source/'scripts/canary_smoke.py')],check=True)
 time.sleep(10)
 if api('/api/rollout')['percent']!=100:raise RuntimeError('Canary rolled back during observation')
 state['promotedImage']=a.image;persist();print('Deployment promoted:',a.image)
except Exception:
 try:api('/api/rollout',{'percent':0})
 finally:print('Deployment failed; requested rollback to last stable release.')
 raise
