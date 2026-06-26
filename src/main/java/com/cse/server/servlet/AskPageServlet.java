package com.cse.server.servlet;

import java.io.IOException;

import com.cse.server.session.SessionService;
import com.cse.server.view.HtmlRenderer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ask question form with streaming answer UI.
 */
public class AskPageServlet extends BaseServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		var session = SessionService.get(req);
		SessionService.onPageVisit(req);
		String stack = session.aiPreferences().effectiveStackId(app().aiSettings().defaultStack());
		StringBuilder body = new StringBuilder();
		body.append("<h2 class=\"title is-4\">Ask</h2>");
		body.append("<p class=\"mb-4\">Ask a question about your indexed content. ");
		body.append("Using stack: <strong>").append(HtmlRenderer.escape(stack)).append("</strong>. ");
		body.append("<a href=\"/settings/ai\">Change AI settings</a></p>");
		body.append("<form id=\"ask-form\" method=\"post\" action=\"/ask/stream\" class=\"box\">");
		body.append(HtmlRenderer.csrfInput(session));
		body.append("<div class=\"field\"><label class=\"label\">Question</label><div class=\"control\">");
		body.append("<textarea class=\"textarea\" name=\"q\" rows=\"3\" required ");
		body.append("placeholder=\"What does the documentation say about…?\"></textarea>");
		body.append("</div></div>");
		body.append("<button class=\"button is-primary\" type=\"submit\">Ask</button>");
		body.append("</form>");
		body.append("<div id=\"ask-answer\" class=\"box\" style=\"display:none\">");
		body.append("<h3 class=\"title is-5\">Answer</h3>");
		body.append("<div id=\"ask-text\"></div>");
		body.append("<h4 class=\"title is-6 mt-4\">Sources</h4>");
		body.append("<ul id=\"ask-sources\"></ul>");
		body.append("</div>");
		body.append("<script>");
		body.append("function addSource(li,s){const a=document.createElement('a');");
		body.append("a.href=s.location||'#';a.textContent=s.title||s.location||'source';");
		body.append("li.appendChild(a);const sn=document.createElement('span');");
		body.append("sn.className='snippet';const t=s.text||'';");
		body.append("sn.textContent=' '+t.slice(0,120)+(t.length>120?'…':'');li.appendChild(sn);}");
		body.append("document.getElementById('ask-form').addEventListener('submit',async e=>{");
		body.append("e.preventDefault();const f=e.target;const fd=new FormData(f);");
		body.append("const box=document.getElementById('ask-answer');const txt=document.getElementById('ask-text');");
		body.append("const src=document.getElementById('ask-sources');box.style.display='block';");
		body.append("txt.textContent='';src.replaceChildren();");
		body.append("const r=await fetch('/ask/stream',{method:'POST',body:fd});");
		body.append("if(!r.ok){txt.textContent='Request failed ('+r.status+').';return;}");
		body.append("const reader=r.body.getReader();const dec=new TextDecoder();let buf='';");
		body.append("while(true){const{done,value}=await reader.read();if(done)break;");
		body.append("buf+=dec.decode(value,{stream:true});let idx;while((idx=buf.indexOf('\\n\\n'))>=0){");
		body.append("const block=buf.slice(0,idx);buf=buf.slice(idx+2);let ev='',data='';");
		body.append("block.split('\\n').forEach(line=>{if(line.startsWith('event: '))ev=line.slice(7);");
		body.append("else if(line.startsWith('data: '))data+=(data?'\\n':'')+line.slice(6);});");
		body.append("if(ev==='retrieval'){const p=JSON.parse(data);(p.sources||[]).forEach(s=>{");
		body.append("const li=document.createElement('li');addSource(li,s);src.appendChild(li);});}");
		body.append("else if(ev==='token'){const p=JSON.parse(data);txt.textContent+=p.text||'';}");
		body.append("else if(ev==='error'){const p=JSON.parse(data);txt.textContent=p.message||'Error';}");
		body.append("}}}");
		body.append("});</script>");
		writeHtml(resp, HtmlRenderer.page(app(), session, "Ask", body.toString()));
	}
}
