"""Checks a running demo deployment over real HTTP, including Kafka publishing."""
import json,time,urllib.request,urllib.error,os,uuid
base=os.environ.get('APP_URL','http://localhost:8083')
token=''
def request(path,body=None):
    data=None if body is None else json.dumps(body).encode()
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    with urllib.request.urlopen(urllib.request.Request(base+'/api'+path,data=data,headers=headers),timeout=15) as response:return json.load(response)
for i in range(90):
    try:
        if request('/health')['status']=='UP':break
    except (OSError,ValueError):time.sleep(2)
else:raise RuntimeError('App did not become healthy')
token=request('/login',{'email':'admin@socialflow.local','password':os.environ.get('ADMIN_PASSWORD','Admin@12345')})['token']
p=request('/posts',{'title':'Deployment smoke '+str(uuid.uuid4())[:8],'content':'A real smoke-test publication in the demo channel.','channel':'Demo LinkedIn'})
request('/posts/'+p['id']+'/schedule',{'dueAt':0,'failDemo':False})
for i in range(40):
    posts=request('/workspace')['posts']
    if any(x['id']==p['id'] and x['status']=='PUBLISHED' for x in posts):
        print('PASS: login, create, schedule, Kafka consume, persistence, analytics');break
    time.sleep(2)
else:raise RuntimeError('Publishing did not complete')
