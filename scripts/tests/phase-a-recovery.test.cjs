const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const source=fs.readFileSync(path.join(__dirname,'../phase-a-recovery.cjs'),'utf8');
function run(operation,data) {
    let saved=data&&JSON.stringify(data), writes=0;
    const commands=[], errors=[];
    const sandbox={process:{argv:['node','recovery',operation,'--serial','virtual-tv','--file','private.json'],exitCode:0},
      console:{log(){},error(message){errors.push(message)}},
      require(name) {
        if(name==='node:fs')return {
          existsSync(){return !!saved},mkdirSync(){},
          readFileSync(){return saved},
          writeFileSync(file,text,options){assert.equal(options.flag,'wx');saved=text;writes++}
        };
        if(name==='node:child_process')return {execFileSync(adb,args){
          const cmd=Array.from(args).slice(3);commands.push(cmd);
          if(cmd.join(' ')==='am get-current-user')return '0\n';
          if(cmd.join(' ')==='getprop ro.build.fingerprint')return 'virtual-build\n';
          if(cmd.includes('resolve-activity'))return 'org.original/.Home\n';
          if(cmd.includes('query-activities'))return '1 activities found:\n  org.original/.Home\n';
          if(cmd[0]==='pm'&&cmd.includes('-d'))return 'package:org.disabled\n';
          return '\n';
        }};
        return require(name);
      }};
    vm.runInNewContext(source,sandbox);
    return {saved,writes,commands,errors,exitCode:sandbox.process.exitCode};
}
const baseline={schema:1,serial:'virtual-tv',user:'0',fingerprint:'virtual-build',
  home:'org.original/.Home',disabled:['package:org.disabled']};
test('backup is read only and never overwrites',()=>{
    const first=run('backup');
    assert.equal(first.exitCode,0);assert.equal(first.writes,1);
    assert.ok(!first.commands.some(c=>c.includes('set-home-activity')));
    const again=run('backup',JSON.parse(first.saved));
    assert.equal(again.exitCode,1);assert.equal(again.writes,0);
});
for(const [field,value] of [['serial','another-tv'],['fingerprint','another-build'],['home','org.missing/.Home']]){
    test('restore rejects mismatched '+field,()=>{
        const result=run('restore',{...baseline,[field]:value});
        assert.equal(result.exitCode,1);
        assert.ok(!result.commands.some(c=>c.includes('set-home-activity')));
    });
}
test('restore targets only the exact original Home and active user',()=>{
    const result=run('restore',baseline);
    assert.equal(result.exitCode,0);
    assert.deepEqual(result.commands.filter(c=>c.includes('set-home-activity')),
      [['cmd','package','set-home-activity','--user','0','org.original/.Home']]);
    assert.ok(!result.commands.some(c=>c.some(x=>['disable-user','enable','uninstall'].includes(x))));
});
