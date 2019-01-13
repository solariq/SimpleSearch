package com.expleague.sensearch.donkey.plain;

import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta;
import com.expleague.sensearch.protobuf.index.IndexUnits.IndexMeta.UriPageMapping;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import java.util.ArrayList;
import java.util.List;

public class IndexMetaBuilder {

  private final int version;
  private final TLongSet termIds = new TLongHashSet();
  private final TLongObjectMap<String> pageUri = new TLongObjectHashMap<>();
  private int totalTokenCount = 0;

  public IndexMetaBuilder(int version) {
    this.version = version;
  }

  public void acceptTermId(long termId) {
    termIds.add(termId);
  }

  public void acceptPage(long pageId, int pageTokenCount, String uri) {
    if (!pageUri.containsKey(pageId)) {
      pageUri.put(pageId, uri);
      totalTokenCount += pageTokenCount;
    }
  }

  public IndexMeta build() {
    List<UriPageMapping> mappings = new ArrayList<>();

    pageUri.forEachEntry((id, uri) -> {
      mappings.add(UriPageMapping.newBuilder().setPageId(id).setUri(uri).build());
      return true;
    });

    return IndexMeta.newBuilder()
        .setVersion(version)
        .setAveragePageSize(1.0 * pageUri.size() / totalTokenCount)
        .setPagesCount(pageUri.size())
        .setVocabularySize(termIds.size())
        .addAllUriPageMappings(mappings)
        .build();
  }
}
