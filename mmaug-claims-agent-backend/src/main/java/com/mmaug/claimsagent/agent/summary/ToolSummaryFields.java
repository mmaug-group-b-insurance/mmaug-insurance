package com.mmaug.claimsagent.agent.summary;
import tools.jackson.databind.JsonNode;
import java.util.*;
/** Only these snapshot fields may become factual summary statements. */
public final class ToolSummaryFields {
 private ToolSummaryFields() {}
 public static Map<String,String> values(String tool,JsonNode data){
  Map<String,String> fields=new LinkedHashMap<>();
  if("get_policy".equals(tool)){
   for(String key:List.of("policyNumber","status","startDate","endDate")){var value=data.path(key);if(value.isString()&&!value.asText().isBlank())fields.put(key,value.asText());}
  }else if("get_coverage".equals(tool)&&data.path("coverages").isArray()) fields.put("coverageRecordCount",Integer.toString(data.path("coverages").size()));
  else if("get_claim_history".equals(tool)&&data.path("claims").isArray()&&data.path("totalClaims").isIntegralNumber()&&data.path("totalClaims").asInt(-1)==data.path("claims").size()) fields.put("historicalClaimCount",Integer.toString(data.path("claims").size()));
  return fields;
 }
}
