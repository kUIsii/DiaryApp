import json

code = open('gen.py', encoding='utf-8').read()
start = code.index('features = [')
end = code.index('\nwith open(')
exec(code[start:end])

# Now build the HTML
html = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>DiaryApp - 功能需求选择器</title>
<link href="https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@300;400;500;600;700&display=swap" rel="stylesheet">
<style>
:root{--bg:#FBF5F0;--ink:#40302A;--ink2:#6C574D;--ink3:#9A8579;--acc:#B89080;--card:#FFFCF9;--green:#5a9a6a;--green-bg:rgba(90,154,106,.08);--serif:'Noto Serif SC',Georgia,'Songti SC',serif;--sans:'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif}
*{box-sizing:border-box;margin:0;padding:0}html{scroll-behavior:smooth}
body{min-height:100vh;color:var(--ink);font:15px/1.8 var(--serif);background:var(--bg)}
body::before{content:"";position:fixed;inset:0;background-image:radial-gradient(circle,rgba(64,48,42,.04) .8px,transparent .8px);background-size:24px 24px;pointer-events:none;z-index:0}
.page{position:relative;z-index:1;max-width:880px;margin:0 auto;padding:40px 24px 200px}
.hero{text-align:center;margin-bottom:40px;opacity:0;animation:fadeUp .8s ease forwards}
.hero .tag{display:inline-block;padding:4px 14px;border-radius:999px;background:rgba(184,144,128,.12);color:var(--acc);font:600 11px/1 var(--sans);letter-spacing:.08em}
.hero h1{margin:16px 0 8px;font:700 clamp(22px,3.5vw,34px)/1.25 var(--serif)}
.hero p{font:300 14px/1.8 var(--serif);color:var(--ink2);max-width:520px;margin:0 auto 20px}
.counters{display:flex;justify-content:center;gap:24px;margin-bottom:10px}
.counters .c{text-align:center}
.counters .c strong{display:block;font:700 24px/1.2 var(--serif)}
.counters .c span{font:400 10.5px/1 var(--sans);color:var(--ink3)}
.tabs{display:flex;gap:4px;justify-content:center;flex-wrap:wrap;margin-bottom:32px;position:sticky;top:8px;z-index:20;padding:10px 0;opacity:0;animation:fadeUp .8s .15s ease forwards}
.tabs::before{content:"";position:absolute;inset:0;background:linear-gradient(180deg,var(--bg) 60%,transparent);pointer-events:none;z-index:-1}
.tab{font:500 12px/1 var(--sans);padding:7px 13px;border-radius:999px;border:1px solid rgba(64,48,42,.08);background:rgba(255,252,249,.6);color:var(--ink3);cursor:pointer;transition:all .25s;user-select:none;backdrop-filter:blur(8px);position:relative}
.tab:hover{background:rgba(184,144,128,.1);color:var(--ink2)}
.tab.active{background:var(--ink);color:var(--bg);border-color:var(--ink)}
.tab .badge{position:absolute;top:-5px;right:-5px;min-width:16px;height:16px;border-radius:999px;background:var(--green);color:#fff;font:700 9px/16px var(--sans);text-align:center;padding:0 4px;display:none}
.tab .badge.show{display:block}
.section{display:none}.section.visible{display:block;animation:fadeIn .35s ease}
.sec-head{margin-bottom:28px;text-align:center}
.sec-head .num{font:700 38px/1 var(--serif);color:rgba(64,48,42,.05);margin-bottom:-8px}
.sec-head h2{font:700 22px/1.3 var(--serif);margin-bottom:3px}
.sec-head .en{font:400 11px/1 var(--sans);color:var(--ink3);letter-spacing:.1em;text-transform:uppercase}
.sec-head .desc{font:300 13px/1.8 var(--serif);color:var(--ink2);max-width:500px;margin:8px auto 0}
.existing{margin-bottom:24px;text-align:center}
.ex-toggle{font:500 12px/1 var(--sans);padding:6px 14px;border-radius:999px;border:1px dashed rgba(64,48,42,.12);background:transparent;color:var(--ink3);cursor:pointer;transition:all .2s}
.ex-toggle:hover{border-color:var(--acc);color:var(--acc)}
.ex-list{display:none;margin-top:12px;flex-wrap:wrap;gap:4px;justify-content:center}
.ex-list.open{display:flex;animation:fadeUp .3s ease}
.ex-list span{font:400 11px/1 var(--sans);padding:3px 9px;border-radius:999px;background:rgba(106,191,138,.08);color:#3d7a52}
.feat{padding:18px 22px;border-radius:6px;background:var(--card);border:1px solid rgba(64,48,42,.05);margin-bottom:12px;cursor:pointer;transition:all .3s;opacity:0;transform:translateY(10px);position:relative;display:flex;gap:14px;align-items:flex-start}
.feat.visible{opacity:1;transform:translateY(0)}
.feat:hover{border-color:rgba(90,154,106,.2);background:#FFFEFB}
.feat.selected{border-color:var(--green);background:var(--green-bg);box-shadow:0 2px 12px rgba(90,154,106,.08)}
.feat .check{width:20px;height:20px;border-radius:50%;border:2px solid rgba(64,48,42,.12);flex-shrink:0;margin-top:2px;transition:all .25s;display:flex;align-items:center;justify-content:center}
.feat.selected .check{border-color:var(--green);background:var(--green)}
.feat.selected .check::after{content:"";display:block;width:6px;height:10px;border:solid #fff;border-width:0 2px 2px 0;transform:rotate(45deg) translate(-1px,-1px)}
.feat .content{flex:1;min-width:0}
.feat .label{font:600 14.5px/1.4 var(--serif);margin-bottom:4px;transition:color .2s}
.feat.selected .label{color:var(--green)}
.feat .body{font:400 12.5px/1.7 var(--serif);color:var(--ink2)}
.feat .src{display:inline-block;margin-top:5px;font:500 10px/1 var(--sans);padding:2px 7px;border-radius:999px;background:rgba(64,48,42,.04);color:var(--ink3)}
.bottom-bar{position:fixed;bottom:0;left:0;right:0;z-index:30;background:rgba(251,245,240,.92);backdrop-filter:blur(12px);border-top:1px solid rgba(64,48,42,.06);padding:12px 24px;display:flex;align-items:center;justify-content:center;gap:16px;opacity:0;transform:translateY(100%);transition:all .35s ease}
.bottom-bar.show{opacity:1;transform:translateY(0)}
.bottom-bar .sel-count{font:500 13px/1 var(--sans);color:var(--ink2)}
.bottom-bar .sel-count strong{color:var(--green);font-weight:700}
.btn-export{font:600 12px/1 var(--sans);padding:9px 20px;border-radius:999px;border:none;background:var(--ink);color:var(--bg);cursor:pointer;transition:all .2s}
.btn-export:hover{opacity:.85}
.btn-export:disabled{opacity:.35;cursor:default}
.btn-clear{font:500 12px/1 var(--sans);padding:9px 16px;border-radius:999px;border:1px solid rgba(64,48,42,.12);background:transparent;color:var(--ink3);cursor:pointer;transition:all .2s}
.btn-clear:hover{border-color:var(--ink3)}
.modal-overlay{display:none;position:fixed;inset:0;z-index:100;background:rgba(64,48,42,.3);backdrop-filter:blur(4px);align-items:center;justify-content:center;padding:20px}
.modal-overlay.show{display:flex;animation:fadeIn .25s ease}
.modal{background:var(--card);border-radius:8px;max-width:680px;width:100%;max-height:80vh;overflow:auto;padding:28px 32px;box-shadow:0 20px 60px rgba(64,48,42,.15);animation:fadeUp .3s ease}
.modal h3{font:700 20px/1.3 var(--serif);margin-bottom:16px}
.modal .req-item{padding:10px 0;border-bottom:1px solid rgba(64,48,42,.05)}
.modal .req-item .req-name{font:600 14px/1.4 var(--serif);color:var(--green)}
.modal .req-item .req-desc{color:var(--ink2);font-size:12.5px;margin-top:2px;line-height:1.7}
.modal .req-item .req-mod{font:500 10px/1 var(--sans);color:var(--ink3);margin-top:4px}
.modal .actions{display:flex;gap:10px;margin-top:20px;justify-content:flex-end;align-items:center}
.modal .btn-copy{font:600 12px/1 var(--sans);padding:9px 20px;border-radius:999px;border:none;background:var(--ink);color:var(--bg);cursor:pointer}
.modal .btn-close{font:500 12px/1 var(--sans);padding:9px 16px;border-radius:999px;border:1px solid rgba(64,48,42,.12);background:transparent;color:var(--ink3);cursor:pointer}
.modal .copied{font:400 12px/1 var(--sans);color:var(--green);opacity:0;transition:opacity .3s}
.modal .copied.show{opacity:1}
.footer{margin-top:40px;text-align:center;font:400 11.5px/1.6 var(--sans);color:var(--ink3)}
@keyframes fadeUp{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:translateY(0)}}
@keyframes fadeIn{from{opacity:0}to{opacity:1}}
@media(max-width:640px){.page{padding:18px 12px 180px}.modal{padding:20px}}
</style>
</head>
<body>
<div class="page">
<div class="hero">
<div class="tag">功能需求选择器</div>
<h1>挑选你想做的功能</h1>
<p>点击卡片勾选你想实现的功能，然后导出为需求清单。选中的内容可以直接交给 AI 开发。</p>
<div class="counters">
<div class="c"><strong id="c1">-</strong><span>已有功能</span></div>
<div class="c"><strong id="c2">-</strong><span>可选建议</span></div>
</div>
</div>
<div class="tabs" id="tabs"></div>
<div id="sections"></div>
<div class="footer">DiaryApp 功能需求选择器</div>
</div>
<div class="bottom-bar" id="bottomBar">
<span class="sel-count">已选 <strong id="selNum">0</strong> 项功能</span>
<button class="btn-clear" onclick="clearAll()">清空</button>
<button class="btn-export" id="btnExport" onclick="openModal()">导出需求清单</button>
</div>
<div class="modal-overlay" id="modalOverlay" onclick="if(event.target===this)closeModal()">
<div class="modal">
<h3>已选功能需求清单</h3>
<div id="reqList"></div>
<div class="actions">
<span class="copied" id="copied">已复制到剪贴板</span>
<button class="btn-close" onclick="closeModal()">关闭</button>
<button class="btn-copy" onclick="copyReq()">复制为文本</button>
</div>
</div>
</div>
<script>
var DATA=__FEATURES__;
var GROUPS=__GROUPS__;
var EXISTING=__EXISTING__;
var activeTab="rec";
var selected={};
function H(s){return s.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;")}
function srcTag(s){if(!s)return"";if(s.startsWith("inspired:"))return'<span class="src">灵感: '+H(s.replace("inspired:",""))+'</span>';return'<span class="src">原创</span>'}
function save(){try{localStorage.setItem("diary_sel_v5",JSON.stringify(selected))}catch(e){}}
function load(){try{var d=localStorage.getItem("diary_sel_v5");if(d)selected=JSON.parse(d)}catch(e){selected={}}}
function toggle(id){if(selected[id]){delete selected[id]}else{selected[id]=true}save();updateUI()}
function clearAll(){selected={};save();updateUI()}
function updateUI(){document.querySelectorAll(".feat").forEach(function(el){var id=el.dataset.id;if(selected[id]){el.classList.add("selected")}else{el.classList.remove("selected")}});var count=Object.keys(selected).length;var bar=document.getElementById("bottomBar");if(count>0){bar.classList.add("show")}else{bar.classList.remove("show")}document.getElementById("selNum").textContent=count;document.getElementById("btnExport").disabled=count===0;GROUPS.forEach(function(g){var tabEl=document.querySelector('.tab[data-g="'+g[0]+'"]');if(!tabEl)return;var badge=tabEl.querySelector(".badge");var items=DATA.filter(function(d){return d.g===g[0]});var selInGroup=items.filter(function(d){return selected[d.id]}).length;badge.textContent=selInGroup;if(selInGroup>0){badge.classList.add("show")}else{badge.classList.remove("show")}})}
function renderTabs(){var t=document.getElementById("tabs");t.innerHTML="";GROUPS.forEach(function(g){var d=document.createElement("span");d.className="tab"+(g[0]===activeTab?" active":"");d.dataset.g=g[0];d.innerHTML=g[1]+" "+g[2]+'<span class="badge">0</span>';d.onclick=function(){activeTab=g[0];renderTabs();renderSection()};t.appendChild(d)})}
function renderSection(){var root=document.getElementById("sections");root.innerHTML="";var items=DATA.filter(function(d){return d.g===activeTab});var ex=EXISTING[activeTab]||[];var G=null;GROUPS.forEach(function(g){if(g[0]===activeTab)G=g});var sec=document.createElement("div");sec.className="section visible";sec.innerHTML='<div class="sec-head"><div class="num">'+G[1]+'</div><h2>'+H(G[3])+'</h2><div class="en">'+G[2]+'</div><div class="desc">'+H(G[4])+'</div></div>';if(ex.length){sec.innerHTML+='<div class="existing"><button class="ex-toggle" onclick="toggleEx(this)">已有能力 '+ex.length+' 项（点击查看）</button><div class="ex-list">'+ex.map(function(e){return'<span>'+H(e)+'</span>'}).join("")+'</div></div>'}sec.innerHTML+=items.map(function(f){var sel=selected[f.id]?" selected":"";return'<div class="feat'+sel+'" data-id="'+f.id+'"><div class="check"></div><div class="content"><div class="label">'+H(f.n)+'</div><div class="body">'+H(f.d)+'</div>'+srcTag(f.s)+'</div></div>'}).join("");root.appendChild(sec);sec.querySelectorAll(".feat").forEach(function(card){card.addEventListener("click",function(){toggle(card.dataset.id)})});requestAnimationFrame(function(){var cards=sec.querySelectorAll(".feat");cards.forEach(function(c,i){setTimeout(function(){c.classList.add("visible")},i*50)})});updateUI()}
function toggleEx(btn){var list=btn.nextElementSibling;var open=list.classList.toggle("open");btn.textContent=open?"收起已有能力":"已有能力 "+list.children.length+" 项（点击查看）"}
function openModal(){var overlay=document.getElementById("modalOverlay");var list=document.getElementById("reqList");var items=DATA.filter(function(d){return selected[d.id]});var grouped={};items.forEach(function(f){if(!grouped[f.g])grouped[f.g]=[];grouped[f.g].push(f)});var html="";GROUPS.forEach(function(g){if(!grouped[g[0]])return;html+='<div style="margin-bottom:16px"><div style="font:600 13px/1.4 var(--serif);color:var(--ink);margin-bottom:8px">'+g[1]+" "+g[2]+" - "+H(g[3])+'</div>';grouped[g[0]].forEach(function(f){html+='<div class="req-item"><div class="req-name">'+H(f.n)+'</div><div class="req-desc">'+H(f.d)+'</div><div class="req-mod">ID: '+f.id+(f.s.startsWith("inspired:")?" | 灵感: "+f.s.replace("inspired:",""):" | 原创")+'</div></div>'});html+='</div>'});list.innerHTML=html||'<p style="color:var(--ink3)">还没有选中任何功能</p>';overlay.classList.add("show")}
function closeModal(){document.getElementById("modalOverlay").classList.remove("show");document.getElementById("copied").classList.remove("show")}
function copyReq(){var items=DATA.filter(function(d){return selected[d.id]});var text="## DiaryApp 功能需求清单\\n\\n共选中 "+items.length+" 项功能\\n\\n";var grouped={};items.forEach(function(f){if(!grouped[f.g])grouped[f.g]=[];grouped[f.g].push(f)});GROUPS.forEach(function(g){if(!grouped[g[0]])return;text+="### "+g[1]+" "+g[2]+" - "+g[3]+"\\n\\n";grouped[g[0]].forEach(function(f){text+="- **"+f.n+"**: "+f.d;if(f.s.startsWith("inspired:"))text+=" (灵感: "+f.s.replace("inspired:","")+")";else text+=" (原创)";text+="\\n"});text+="\\n"});text+="---\\n生成时间: "+new Date().toLocaleString("zh-CN");navigator.clipboard.writeText(text).then(function(){var el=document.getElementById("copied");el.classList.add("show");setTimeout(function(){el.classList.remove("show")},2000)}).catch(function(){})}
load();var totalCurr=0;Object.values(EXISTING).forEach(function(a){totalCurr+=a.length});document.getElementById("c1").textContent=totalCurr;document.getElementById("c2").textContent=DATA.length;renderTabs();renderSection();
</script>
</body>
</html>'''

html = html.replace('__FEATURES__', json.dumps(features, ensure_ascii=False))
html = html.replace('__GROUPS__', json.dumps(groups, ensure_ascii=False))
html = html.replace('__EXISTING__', json.dumps(existing, ensure_ascii=False))

with open('feature-analysis.html', 'w', encoding='utf-8') as f:
    f.write(html)
print(f'DONE: {len(html)} chars, {len(features)} features')
