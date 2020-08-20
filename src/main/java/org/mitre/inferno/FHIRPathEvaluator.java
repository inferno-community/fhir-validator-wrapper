package org.mitre.inferno;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.Base;
import org.hl7.fhir.r5.model.DataType;
import org.hl7.fhir.r5.model.Resource;
import org.hl7.fhir.r5.utils.FHIRPathEngine;

public class FHIRPathEvaluator extends FHIRPathEngine {

  public FHIRPathEvaluator() throws IOException {
    super(new SimpleWorkerContext());
  }

  @Override
  public String evaluateToString(Base base, String path) {
    List<Base> result = this.evaluate(base, path);
    return '['
        + result.stream().map(this::baseToJson).collect(Collectors.joining(","))
        + ']';
  }

  private String baseToJson(Base item) {
    return String.format("{\"type\":\"%s\",\"value\":%s}", item.fhirType(), baseToString(item));
  }

  String baseToString(Base item) {
    String type = item.fhirType();
    if (item.isPrimitive()) {
      String value = item.primitiveValue();
      switch (type) {
        case "boolean":
        case "integer":
        case "decimal":
          return value;
        default: // TODO: handle date, dateTime, etc. appropriately
          return new Gson().toJson(value);
      }
    }
    try {
      if (item.isResource()) {
        return new JsonParser().composeString((Resource) item);
      } else if (item instanceof DataType) {
        return new JsonParser().composeString((DataType) item, type);
      }
    } catch (IOException e) {
      throw new FHIRException(String.format("Failed to compose item of type [%s].", type), e);
    }
    throw new IllegalArgumentException(String.format("[%s] was not a Resource or DataType.", type));
  }
}