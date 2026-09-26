# Validates a response body against a swagger schema.
# jq -r --slurpfile sw loops/backend-dev/outputs/openapi.json --arg s Task|[Task]|Problem --argjson code 200 -f this.jq body
# Prints "ok" or the first problems. Problem (RFC 9457) comes from specs/001-quickflow-backend/contracts/openapi.yaml.
def S: $sw[0].components.schemas + {Problem: {type: "object", required: ["title", "status"], extensible: true,
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
      end
    elif $t == "array" then
      if ($v | type) != "array" then ["\($p): not an array"]
      else [range(0; $v | length) as $i | ($v[$i] | chk($s.items; "\($p)[\($i)]"))[]] end
    elif $t == "integer" then
      if ($v | type) != "number" or ($v | floor) != $v then ["\($p): \($v) not integer"]
      elif ($s | has("minimum")) and $v < $s.minimum then ["\($p): \($v) < \($s.minimum)"]
      elif ($s | has("maximum")) and $v > $s.maximum then ["\($p): \($v) > \($s.maximum)"]
      else [] end
    elif $t == "boolean" then (if ($v | type) != "boolean" then ["\($p): \($v) not boolean"] else [] end)
    elif $t == "string" then
      if ($v | type) != "string" then ["\($p): \($v) not string"]
      elif ($s | has("enum")) and (($s.enum | index($v)) | not) then ["\($p): \($v) not in \($s.enum)"]
      elif $s.format == "date" and ($v | test("^\\d{4}-\\d{2}-\\d{2}$") | not) then ["\($p): \($v) not date"]
      elif $s.format == "date-time" and ($v | test("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?(Z|[+-]\\d{2}:\\d{2})$") | not)
        then ["\($p): \($v) not date-time with offset"]
      else [] end
    else [] end
  end;
($s | startswith("[")) as $arr | ($s | ltrimstr("[") | rtrimstr("]")) as $n |
(chk(if $arr then {type: "array", items: {"$ref": "#/components/schemas/\($n)"}} else {"$ref": "#/components/schemas/\($n)"} end; "$")
 + (if $n == "Problem" and .status != $code then ["$.status \(.status) vs HTTP \($code)"] else [] end)) as $e |
if ($e | length) == 0 then "ok" else ($e[:5] | join("; ")) end
