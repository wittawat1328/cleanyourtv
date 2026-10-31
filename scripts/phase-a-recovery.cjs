#!/usr/bin/env node
// Dependency-free Windows/macOS/Linux recovery. Baselines are compatible with phase-a-recovery.py.
const fs = require('node:fs');
const path = require('node:path');
const {execFileSync} = require('node:child_process');
function main() {
    const [operation, ...args] = process.argv.slice(2);
    if (!['backup','restore','verify'].includes(operation)) throw Error('Use backup / restore / verify --serial <tv-serial> --file <private-backup.json> [--adb <path>]');
    const options = {};
    for (let i=0; i<args.length; i+=2) {
        if (!['--serial','--file','--adb'].includes(args[i]) || !args[i+1] || options[args[i]]) throw Error('Invalid arguments');
        options[args[i]]=args[i+1];
    }
    const serial=options['--serial'], file=options['--file'], adb=options['--adb']||'adb';
    if (!serial || !file) throw Error('--serial and --file are required');
    const sh=(...cmd)=>execFileSync(adb,['-s',serial,'shell',...cmd],{encoding:'utf8',windowsHide:true}).trim();
    const lines=text=>text.split(/\r?\n/).map(s=>s.trim()).filter(Boolean);
    const user=sh('am','get-current-user');
    const home=()=>lines(sh('cmd','package','resolve-activity','--user',user,'--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')).at(-1);
    if (operation==='backup') {
        if (fs.existsSync(file)) throw Error('Backup exists. Use another path; original baseline must not be overwritten.');
        const original=home();
        if (!original || !original.includes('/') || original.includes('ResolverActivity') || original.startsWith('com.wittawat.cleanyourtv.launcher/')) throw Error('Select an unambiguous original Home before backup.');
        const data={schema:1,serial,user,home:original,fingerprint:sh('getprop','ro.build.fingerprint'),
            properties:sh('getprop'),disabled:lines(sh('pm','list','packages','-d','--user',user)),
            packages:lines(sh('pm','list','packages','--user',user)),
            settings_system:sh('settings','--user',user,'list','system'),
            settings_secure:sh('settings','--user',user,'list','secure'),
            settings_global:sh('settings','list','global')};
        fs.mkdirSync(path.dirname(path.resolve(file)),{recursive:true});
        fs.writeFileSync(file,JSON.stringify(data,null,2),{encoding:'utf8',flag:'wx',mode:0o600});
        console.log('Original Home and read-only baseline saved. Keep the backup private.');
        return;
    }
    const data=JSON.parse(fs.readFileSync(file,'utf8'));
    if (data.schema!==1 || data.serial!==serial || data.user!==user) throw Error('Backup belongs to another device/user.');
    if (data.fingerprint!==sh('getprop','ro.build.fingerprint')) throw Error('Firmware changed. Inspect the original Home manually.');
    const homes=lines(sh('cmd','package','query-activities','--brief','--user',user,'-a','android.intent.action.MAIN','-c','android.intent.category.HOME'));
    if (!homes.includes(data.home)) throw Error('Original Home is unavailable. No package changes made.');
    if (operation==='restore') {
        sh('cmd','package','set-home-activity','--user',user,data.home);
        sh('am','start','--user',user,'-a','android.intent.action.MAIN','-c','android.intent.category.HOME');
    }
    if (home()!==data.home) throw Error('Default Home differs from baseline.');
    const disabled=lines(sh('pm','list','packages','-d','--user',user)).sort();
    if (JSON.stringify(disabled)!==JSON.stringify([...data.disabled].sort())) throw Error('Disabled packages differ from baseline; inspect manually.');
    console.log('PASS: Original Default Home and disabled-package baseline match.');
}
try { main(); } catch (error) { console.error(error.message); process.exitCode=1; }
