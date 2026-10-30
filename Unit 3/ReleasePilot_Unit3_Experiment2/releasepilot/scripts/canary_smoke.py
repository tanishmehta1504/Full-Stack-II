#!/usr/bin/env python3
"""Exercise an already-running gateway. --compose-fault injects a local regression."""
import argparse,json,time,urllib.request,urllib.error,uuid,subprocess,os
p=argparse.ArgumentParser();p.add_argument('--base',default='http://localhost:8082');p.add_argument('--compose-fault',action='store_true');args=p.parse_args()
def call(path,body=None,key=None):
 headers={'X-Lab-Request':'releasepilot'}
 if body is not None:headers['Content-Type']='application/json'
 if key:headers['X-Canary-Key']=key
 request=urllib.request.Request(args.base+path,data=None if body is None else json.dumps(body).encode(),headers=headers)
 with urllib.request.urlopen(request,timeout=8) as response:return json.load(response),response.headers
call('/api/rollout',{'percent':0});call('/api/rollout',{'percent':10})
tracks={'stable':0,'canary':0}
for i in range(200):
 body,headers=call('/traffic/api/quote?quantity=2',key='smoke-'+str(i));assert body['total']==998
 tracks[headers['X-Release-Track']]+=1
assert tracks['stable']>0 and tracks['canary']>=5,tracks
call('/api/rollout',{'percent':100})
for i in range(10):assert call('/traffic/api/quote?quantity=2',key=str(i))[1]['X-Release-Track']=='canary'
print('PASS: real upstream routing, canary samples, and 100% promotion',tracks)
if args.compose_fault:
 try:
  subprocess.run(['docker','compose','up','-d','--wait','canary'],env={**os.environ,'CANARY_FAIL_QUOTES':'true'},check=True)
  deadline=time.time()+35
  while time.time()<deadline:
   if call('/api/rollout')[0]['percent']==0:break
   time.sleep(1)
  else:raise AssertionError('Automatic rollback did not occur')
  body,headers=call('/traffic/api/quote?quantity=2',key='after-failure');assert headers['X-Release-Track']=='stable' and body['total']==998
  print('PASS: injected candidate regression automatically rolls back to working stable')
 finally:subprocess.run(['docker','compose','up','-d','--wait','canary'],env={**os.environ,'CANARY_FAIL_QUOTES':'false'},check=True)
