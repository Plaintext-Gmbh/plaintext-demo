package ch.plaintext.demo.tabelle;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.boot.plugins.security.PlaintextSecurityHolder;
import ch.plaintext.boot.search.SearchProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoSearchProviderTest {

    @Test
    void findsThePageAndTheProductsOfTheCurrentTenant() {
        PlaintextSecurity security = mock(PlaintextSecurity.class);
        when(security.getMandat()).thenReturn("extra");
        new PlaintextSecurityHolder().setDelegate(security);
        DemoProduct tent = new DemoProduct();
        tent.setSku("EX-1133");
        tent.setName("Tent 2P A36");
        tent.setCategory("Sports");
        DemoProductRepository repository = mock(DemoProductRepository.class);
        when(repository.findByMandatAndDeletedFalseOrderBySku("extra")).thenReturn(List.of(tent));
        DemoSearchProvider provider = new DemoSearchProvider(repository);

        assertEquals("Configurable table", provider.search("tabelle", 10).get(0).getTitle());
        List<SearchProvider.SearchHit> hits = provider.search("tent", 10);
        assertEquals(1, hits.size());
        assertTrue(hits.get(0).getTitle().startsWith("EX-1133"));
        assertEquals("demo-table.html", hits.get(0).getLink());
        assertTrue(provider.search("x", 10).isEmpty(), "one letter is no search");
    }
}
