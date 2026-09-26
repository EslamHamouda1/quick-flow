# Validates response bodies against the swagger schemas (batch). Based on the 001-quickflow suite validator;
# 002-us-tags adds minLength/maxLength/minItems/maxItems/uniqueItems and Task.tags lower case + alphabetical.
# jq -rn '<this program>' <swagger> <expect.json {"NN": ["Schema"|"[Schema]"|"Problem"|"-", code]}> b/NN.json...
# (jq -f needs approval in loop sessions, so the program text is passed inline.)
# Prints "NN<TAB>schema<TAB>ok|first problems" per body file whose schema isn't "-" (no-body/unchecked).
# Problem (RFC 9457) comes from specs/001-quickflow-backend/contracts/openapi.yaml.
input as $swd | input as $map |
def S: $swd.components.schemas + {Problem: {type: "object", required: ["title", "status"], extensible: true,
  properties: {type: {type: "string"}, title: {type: "string"}, status: {type: "integer"}, detail: {type: "string"},
    instance: {type: "string"}, errors: {type: "array", items: {type: "object", required: ["field", "message"],
      properties: {field: {type: "string"}, message: {type: "string"}}}}}}};
def res: if type == "object" and has("$ref") then (S[.["$ref"] | split("/") | last] | res) else . end;
def types: (.type // null) | if type == "array" then . elif . == null then [] else [.] end;
def chk($s0; $p):
  . as $v | ($s0 | res) as $s | ($s | types) as $ts |
  if $v == null then (if ($ts | index("null")) then [] else ["\($p): null not allowed"] end)
  else ([$ts[] | select(. != "null")][0]) as $t |
    if $t == "object" then
      if ($v | type) != "object" then ["\($p): not an object"]
      else ($s.properties // {}) as $pr |
        [($s.required // [])[] | . as $k | select(($v | has($k)) | not) | "\($p): missing \($k)"]
        + (if $s.extensible then [] else [$v | keys[] | . as $k | select(($pr | has($k)) | not) | "\($p): extra \($k)"] end)
        + [$v | to_entries[] | .key as $k | select($pr | has($k)) | (.value | chk($pr[$k]; "\($p).\($k)"))[]]
        + (if $s == S.Task and ($v.tags | type) == "array" and $v.tags != ($v.tags | map(ascii_downcase) | sort)
           then ["\($p).tags: \($v.tags) not lower case and alphabetical"] else [] end)
      end
    elif $t == "array" then
      if ($v | type) != "array" then ["\($p): not an array"]
      else (if ($s | has("maxItems")) and ($v | length) > $s.maxItems then ["\($p): \($v | length) items > \($s.maxItems)"] else [] end)
        + (if ($s | has("minItems")) and ($v | length) < $s.minItems then ["\($p): \($v | length) items < \($s.minItems)"] else [] end)
        + (if $s.uniqueItems and ($v | unique | length) != ($v | length) then ["\($p): items not unique"] else [] end)
        + [range(0; $v | length) as $i | ($v[$i] | chk($s.items; "\($p)[\($i)]"))[]] end
    elif $t == "integer" then
      if ($v | type) != "number" or ($v | floor) != $v then ["\($p): \($v) not integer"]
      elif ($s | has("minimum")) and $v < $s.minimum then ["\($p): \($v) < \($s.minimum)"]
      elif ($s | has("maximum")) and $v > $s.maximum then ["\($p): \($v) > \($s.maximum)"]
      else [] end
    elif $t == "boolean" then (if ($v | type) != "boolean" then ["\($p): \($v) not boolean"] else [] end)
    elif $t == "string" then
      if ($v | type) != "string" then ["\($p): \($v) not string"]
      elif ($s | has("enum")) and (($s.enum | index($v)) | not) then ["\($p): \($v) not in \($s.enum)"]
      elif ($s | has("minLength")) and ($v | length) < $s.minLength then ["\($p): \($v) shorter than \($s.minLength)"]
      elif ($s | has("maxLength")) and ($v | length) > $s.maxLength then ["\($p): \($v) longer than \($s.maxLength)"]
      elif $s.format == "date" and ($v | test("^\\d{4}-\\d{2}-\\d{2}$") | not) then ["\($p): \($v) not date"]
      elif $s.format == "date-time" and ($v | test("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?(Z|[+-]\\d{2}:\\d{2})$") | not)
        then ["\($p): \($v) not date-time with offset"]
      else [] end
    else [] end
  end;
def validate($s; $code):
  ($s | startswith("[")) as $arr | ($s | ltrimstr("[") | rtrimstr("]")) as $n |
  (chk(if $arr then {type: "array", items: {"$ref": "#/components/schemas/\($n)"}} else {"$ref": "#/components/schemas/\($n)"} end; "$")
   + (if $n == "Problem" and .status != $code then ["$.status \(.status) vs HTTP \($code)"] else [] end)) as $e |
  if ($e | length) == 0 then "ok" else ($e[:5] | join("; ")) end;
inputs | . as $b | (input_filename | split("/") | last | rtrimstr(".json")) as $k |
select($map | has($k)) | $map[$k] as [$s, $code] | select($s != "-") | "\($k)\t\($s)\t\($b | validate($s; $code))"
