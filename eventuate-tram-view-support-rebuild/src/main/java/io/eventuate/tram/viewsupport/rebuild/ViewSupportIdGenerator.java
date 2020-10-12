package io.eventuate.tram.viewsupport.rebuild;

import io.eventuate.common.id.IdGenerator;
import io.eventuate.common.id.Int128;
import io.eventuate.common.jdbc.EventuateCommonJdbcOperations;
import io.eventuate.common.jdbc.EventuateSchema;

import java.util.Collections;

public class ViewSupportIdGenerator {

  private EventuateSchema eventuateSchema;
  private EventuateCommonJdbcOperations eventuateCommonJdbcOperations;
  private IdGenerator idGenerator;

  private String anchorMessageId;

  public ViewSupportIdGenerator(EventuateSchema eventuateSchema,
                                EventuateCommonJdbcOperations eventuateCommonJdbcOperations,
                                IdGenerator idGenerator) {

    this.eventuateSchema = eventuateSchema;
    this.eventuateCommonJdbcOperations = eventuateCommonJdbcOperations;
    this.idGenerator = idGenerator;
  }

  public String generateId() {
    if (anchorMessageId == null) {
      anchorMessageId = generateAnchorMessageId();
    } else {
      anchorMessageId = idGenerator
              .incrementIdIfPossible(Int128.fromString(anchorMessageId))
              .map(Int128::asString)
              .orElseGet(this::generateAnchorMessageId);
    }

    return anchorMessageId;
  }

  private String generateAnchorMessageId() {
      return eventuateCommonJdbcOperations.insertPublishedMessageIntoMessageTable(idGenerator,
              "\"ANCHOR-MESSAGE-FOR-SNAPSHOT-DATABASE-ID-GENERATION\"",
              "CDC-IGNORED",
              Collections.emptyMap(),
              eventuateSchema);
  }
}
