# -*- coding: utf-8 -*-
import json

with open('database_schema.json', 'r', encoding='utf-8') as f:
    schema = json.load(f)

def col_type(c):
    t = c.get('type', '')
    if c.get('max_length'):
        return "{} ({})".format(t, c['max_length'])
    if t in ('numeric', 'decimal') and c.get('precision') is not None:
        s = c.get('scale', 0)
        return "{}({}, {})".format(t, c['precision'], s) if s else "{}({})".format(t, c['precision'])
    if t in ('bigint', 'integer', 'smallint') and c.get('precision'):
        return "{} ({})".format(t, c['precision'])
    if t == 'double precision' and c.get('precision'):
        return "{} ({})".format(t, c['precision'])
    return t

lines = []
for table, data in schema.items():
    cols = data.get('columns', [])
    fks = {fk['column']: (fk['references_table'], fk['references_column']) for fk in data.get('foreign_keys', [])}
    lines.append('')
    lines.append('## {}'.format(table))
    lines.append('| Column | Type | Nullable | Default | FK |')
    lines.append('|--------|------|----------|---------|-----|')
    for c in cols:
        name = c['name']
        nullable = 'YES' if c.get('nullable') == 'YES' else 'NO'
        default = c.get('default') or '-'
        if default != '-' and len(str(default)) > 45:
            default = str(default)[:42] + '...'
        ref = fks.get(name)
        fk_str = "-> {}.{}".format(ref[0], ref[1]) if ref else ('(no FK)' if name.endswith('_id') else '')
        lines.append("| {} | {} | {} | {} | {} |".format(name, col_type(c), nullable, default, fk_str))
    pk_cols = [p['column'] for p in data.get('primary_keys', [])]
    lines.append('')
    lines.append('PK: {}'.format(pk_cols))
    if data.get('foreign_keys'):
        lines.append('FKs: ' + ', '.join("{} -> {}.{}".format(fk['column'], fk['references_table'], fk['references_column']) for fk in data['foreign_keys']))
    if data.get('unique_constraints'):
        lines.append('Unique: ' + ', '.join(u['column'] for u in data['unique_constraints']))

print('\n'.join(lines))
