package ch.plaintext.demo.tabelle;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.boot.plugins.security.PlaintextSecurityHolder;
import ch.plaintext.boot.table.TableColumn;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoTableBackingBeanTest {

    @Test
    void initKeepsKeysRequiredColumnsAndDefaultVisibility() {
        DemoTableBackingBean bean = new DemoTableBackingBean();
        bean.init(); // no store, no i18n: labels stay German, nothing is persisted

        List<TableColumn> columns = DemoTableBackingBean.COLUMNS;
        assertEquals(21, columns.size());
        assertTrue(bean.getAnzeige().isPflichtspalte("sku"), "SKU is a required column");
        assertTrue(bean.getAnzeige().isPflichtspalte("name"), "name is a required column");
        assertFalse(bean.getAnzeige().isVisible("notes"), "notes starts hidden");
        assertTrue(bean.getAnzeige().isVisible("price"));
        assertEquals("Produktname", bean.getAnzeige().columnLabel("name"));
        assertEquals(7, columns.stream().filter(c -> !c.isDefaultVisible()).count(), "the page says seven columns start hidden");
    }

    @Test
    void statusCodesBecomeTranslatableGermanTexts() {
        DemoTableBackingBean bean = new DemoTableBackingBean();
        assertEquals("Verfuegbar", bean.statusText("AVAILABLE"));
        assertEquals("Knapp", bean.statusText("LOW_STOCK"));
        assertEquals("Bestellt", bean.statusText("ORDERED"));
        assertEquals("Ausgelaufen", bean.statusText("DISCONTINUED"));
        assertEquals("OTHER", bean.statusText("OTHER"));
        assertEquals("", bean.statusText(null));
    }

    @Test
    void loadReadsTheRowsOfTheCurrentMandate() {
        PlaintextSecurity security = mock(PlaintextSecurity.class);
        when(security.getMandat()).thenReturn("extra");
        new PlaintextSecurityHolder().setDelegate(security);
        DemoProductRepository repository = mock(DemoProductRepository.class);
        DemoProduct row = new DemoProduct();
        row.setSku("EX-1007");
        when(repository.findByMandatAndDeletedFalseOrderBySku("extra")).thenReturn(List.of(row));
        DemoTableBackingBean bean = new DemoTableBackingBean();
        ReflectionTestUtils.setField(bean, "repository", repository);

        bean.load();

        assertEquals(List.of(row), bean.getProducts());
        assertEquals("extra", bean.getMandat());
    }
}
