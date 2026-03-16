# Pack Generator

This tool generates a resource pack from the YAML content in
`server/plugins/MagicAcademy/items/*.yml`.

## Run

```bash
python tools/packgen/packgen.py
```

Outputs:
- `server/resource_pack/MagicAcademy/` (unpacked)
- `server/resource_pack/MagicAcademy.zip`
- Updates `server/server.properties` if `server/resource_pack/pack.yml` has a URL

## Config

`server/resource_pack/pack.yml` controls pack format and URL. If it does not
exist, the tool creates a default template.
