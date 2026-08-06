import re, json, sys

def parse_strings(path):
    d = {}
    with open(path, encoding='utf-8') as f:
        content = f.read()
    # Match "key" = "value"; allowing escaped quotes
    for m in re.finditer(r'"((?:[^"\\]|\\.)*)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;', content):
        key = m.group(1)
        val = m.group(2).replace('\\"','"').replace('\\n','\n')
        d[key] = val
    return d

base = "/Users/uebetsunawayuuya/MapRoulette/MapRoulette/Base"
ja = parse_strings(f"{base}/ja.lproj/Localizable.strings")
en = parse_strings(f"{base}/en.lproj/Localizable.strings")

# Build ja_value -> en_value using shared keys
ja2en = {}
collisions = {}
for k, jav in ja.items():
    if k in en:
        env = en[k]
        if jav in ja2en and ja2en[jav] != env:
            collisions.setdefault(jav, set()).add(ja2en[jav])
            collisions[jav].add(env)
        ja2en[jav] = env

print(f"ja keys: {len(ja)}, en keys: {len(en)}, ja->en pairs: {len(ja2en)}", file=sys.stderr)
print(f"collisions (same JA, different EN): {len(collisions)}", file=sys.stderr)
for jav, ens in list(collisions.items())[:10]:
    print(f"  COLLISION {jav!r} -> {ens}", file=sys.stderr)

with open("/private/tmp/claude-501/-Users-uebetsunawayuuya-AndroidStudioProjects-JapanTripMap/0dc91cd7-5430-4083-b07e-a37e9208352a/scratchpad/ja2en.json","w",encoding='utf-8') as f:
    json.dump(ja2en, f, ensure_ascii=False, indent=1)
