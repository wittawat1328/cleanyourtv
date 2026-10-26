'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {pathToFileURL}=require('node:url'),{chromium}=require('playwright');
(async()=>{
 const out=path.resolve(__dirname,'../artifacts/web-preview');fs.mkdirSync(out,{recursive:true});
 const browser=await chromium.launch();
 try{
  const page=await browser.newPage({viewport:{width:1440,height:900}});
  const external=[];page.on('request',r=>{if(!r.url().startsWith('file://'))external.push(r.url());});
  await page.goto(pathToFileURL(path.resolve(__dirname,'index.html')).href);
  assert.match(await page.title(),/CleanYourTV/);
  assert.equal(await page.locator('.tile.active').getAttribute('data-name'),'YouTube');
  await page.locator('#tv').screenshot({path:path.join(out,'tv-default.png')});
  await page.keyboard.press('ArrowRight');await page.keyboard.press('ArrowRight');
  assert.equal(await page.locator('#hero').textContent(),'Prime Video');
  await page.getByRole('button',{name:'OK',exact:true}).click();
  assert.equal(await page.locator('#overlay').evaluate(n=>n.classList.contains('on')),true);
  await page.keyboard.press('Escape');
  assert.equal(await page.locator('#overlay').evaluate(n=>n.classList.contains('on')),false);
  await page.locator('[data-theme="aurora"]').click();
  assert.equal(await page.locator('#tv').evaluate(n=>n.classList.contains('aurora')),true);
  await page.locator('#tv').screenshot({path:path.join(out,'tv-aurora.png')});
  await page.screenshot({path:path.join(out,'preview-page.png'),fullPage:true});
  assert.deepEqual(external,[],'offline-only');
  fs.writeFileSync(path.join(out,'report.json'),JSON.stringify({status:'PASS',checks:['offline','remote navigation','OK','Back','theme']},null,2));
  console.log('PASS: web preview');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
