/* Nori Android additive bridge V2. V10 ZIP stays untouched. */
(function(g){
'use strict';
if(g.__NORI_ANDROID_FIX_V2__)return;
g.__NORI_ANDROID_FIX_V2__=true;
const EDGE='https://qrlvkxewyoceykaquapv.supabase.co/functions/v1/nori-brain';
const nativeFetch=g.fetch.bind(g);
function urlOf(input){try{return typeof input==='string'?input:String(input&&input.url||'')}catch(_){return ''}}
function isBackend(url){try{return /^\\/api\\/(nori|brain|brain\\.js)$/.test(new URL(url,g.location&&g.location.href||'https://localhost/').pathname)}catch(_){return /\\/api\\/(nori|brain|brain\\.js)(?:\\?|$)/.test(String(url))}}
g.fetch=function(input,init){
 const url=urlOf(input),method=String((init&&init.method)||(input&&input.method)||'GET').toUpperCase();
 if(method!=='POST'||!isBackend(url))return nativeFetch(input,init);
 const headers=new Headers((init&&init.headers)||(input&&input.headers)||{});
 try{const token=typeof g.noriSyncGetAccessToken==='function'?g.noriSyncGetAccessToken():'';if(token)headers.set('Authorization','Bearer '+token)}catch(_){}
 headers.set('Content-Type','application/json');headers.set('X-Nori-Android','1');
 return nativeFetch(EDGE,Object.assign({},init||{},{method:'POST',headers,cache:'no-store'}));
};
function voice(){return g.Capacitor&&g.Capacitor.Plugins&&g.Capacitor.Plugins.NoriVoice}
g.noriBackgroundVoiceAvailable=()=>!!voice();
g.noriBackgroundVoiceStart=o=>{const p=voice();return p&&p.start?p.start(Object.assign({wakeWord:'nori',language:'en-US',doubleClap:true},o||{})):Promise.reject(new Error('Native voice is unavailable.'))};
g.noriBackgroundVoiceStop=()=>{const p=voice();return p&&p.stop?p.stop():Promise.resolve()};
g.noriBackgroundVoiceSpeak=t=>{const p=voice();return p&&p.speak?p.speak({text:String(t||'')}):Promise.reject(new Error('Native voice is unavailable.'))};

function patchSpeech(){
 if(typeof g.noriSpeak!=='function'||g.__NORI_ANDROID_SPEECH_PATCH__)return;
 g.__NORI_ANDROID_SPEECH_PATCH__=true;const original=g.noriSpeak;
 g.noriSpeak=function(text,options){
  const p=voice();
  if(p&&p.speak&&text){try{Promise.resolve(p.speak({text:String(text)})).catch(()=>original(text,options));return}catch(_){}}
  return original(text,options);
 };
}
function refreshAuth(){
 try{if(g.NoriAuthGate&&typeof g.NoriAuthGate.refresh==='function')g.NoriAuthGate.refresh()}catch(_){}
}
g.addEventListener('nori:auth-ready',refreshAuth);
g.addEventListener('DOMContentLoaded',function(){
 patchSpeech();setTimeout(()=>{patchSpeech();refreshAuth()},250);setTimeout(()=>{patchSpeech();refreshAuth()},1200);
});
})(window);