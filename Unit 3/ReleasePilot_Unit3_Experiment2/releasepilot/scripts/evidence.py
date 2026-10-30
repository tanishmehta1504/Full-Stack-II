#!/usr/bin/env python3
"""Create factual build evidence from Maven XML; missing suites are NOT_RUN."""
import argparse,datetime,json,pathlib,os,xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--output',default='src/main/resources/build-evidence.json');args=p.parse_args()
groups={'Unit tests':[],'MockMvc integration':[],'Pact consumer':[],'Pact provider':[],'PostgreSQL Testcontainers':[]}
for report in pathlib.Path('target').glob('*-reports/TEST-*.xml'):
 root=ET.parse(report).getroot();name=root.attrib['name']
 group=next((g for term,g in [('QuoteServiceTest','Unit tests'),('ApiIntegrationTest','MockMvc integration'),('DashboardConsumerPactTest','Pact consumer'),('QuoteProviderIT','Pact provider'),('PostgresContainerCheck','PostgreSQL Testcontainers')] if term in name),None)
 if group:groups[group].append({k:int(root.attrib.get(k,0)) for k in ['tests','failures','errors','skipped']})
suites=[]
for name,records in groups.items():
 stats={k:sum(r[k] for r in records) for k in ['tests','failures','errors','skipped']}
 status='NOT_RUN' if not records else 'FAILED' if stats['failures']+stats['errors'] else 'SKIPPED' if stats['skipped'] else 'PASSED'
 suites.append({'name':name,'status':status,**stats})
result={'revision':os.environ.get('GITHUB_SHA','local'),'status':'RECORDED','recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'source':'Maven JUnit XML reports; this is build evidence, not a live GitHub Actions connection.','suites':suites}
path=pathlib.Path(args.output);path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
