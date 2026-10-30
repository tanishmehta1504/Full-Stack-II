import re

path = r'c:\Users\tanis\Desktop\Full Stack-II\Unit 3\PostFlow_Unit3_Experiment1\postflow\src\main\resources\static\app.js'

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the replay+refresh block with replay+delete-confirm+delete+refresh block
old = (
    " if(a==='replay'){await api('/dead-letters/'+encodeURIComponent(id)+'/replay','POST');"
    "closeModal();toast('Fault removed and replay queued. Follow the event in the queue.');}\n"
    " if(['duplicate','scenario','replay'].includes(a))await refresh();\n"
    " }catch(e){toast(e.message,true);}finally{button.disabled=false;}}"
)

new = (
    " if(a==='replay'){await api('/dead-letters/'+encodeURIComponent(id)+'/replay','POST');"
    "closeModal();toast('Fault removed and replay queued. Follow the event in the queue.');}\n"
    " if(a==='delete-confirm'){const title=button.dataset.title;"
    "modal('Delete this post?',`<p class=\"info\">This will permanently remove <strong>${esc(title)}</strong>"
    " and all its pipeline records. This action cannot be undone.</p>"
    "<div class=\"modal-actions\">"
    "<button class=\"button secondary\" data-action=\"close\">Cancel</button>"
    "<button class=\"button danger\" data-action=\"delete\" data-id=\"${esc(id)}\">&#128465; Yes, delete it</button>"
    "</div>`,'REMOVE EVENT');}\n"
    " if(a==='delete'){await api('/events/'+encodeURIComponent(id),'DELETE');"
    "closeModal();toast('Post deleted successfully.');navigate('events');}\n"
    " if(['duplicate','scenario','replay','delete'].includes(a))await refresh();\n"
    " }catch(e){toast(e.message,true);}finally{button.disabled=false;}}"
)

if old in content:
    content = content.replace(old, new, 1)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('SUCCESS: delete action handler patched')
else:
    print('ERROR: target string not found!')
    # Debug: show what we have around line 43
    lines = content.split('\n')
    for i, line in enumerate(lines[40:47], start=41):
        print(f'Line {i}: {repr(line[:150])}')
