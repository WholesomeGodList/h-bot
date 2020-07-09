package bot.sites.nhentai;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NHFetcherTest {
	@Test
	void testMetadata() throws IOException {
		NHFetcher checker = new NHFetcher("https://nhentai.net/g/187539/", null);
		NHFetcher checker2 = new NHFetcher("https://nhentai.net/g/171091/", null);
		assertEquals("A Monster's Hospitality", checker2.getTitle());
		assertEquals("Hisasi", String.join("", checker.getArtists()));
		assertEquals(2, checker.getChars().size());
		assertEquals("English", checker.getLanguage());
		assertEquals(7, checker.getTags().size());
		assertTrue(checker.getTags().contains("ahegao"));
		assertTrue(checker.getFavorites() > 6000);
		assertEquals(2, checker.getChars().size());
		assertTrue(checker.getChars().contains("erina nakiri"));
		assertEquals("Erina-sama's Love Laboratory. 2", checker.getTitle());
		assertEquals("neko wa manma ga utsukushii", String.join("", checker.getGroups()));
		assertEquals("shokugeki no soma", String.join("", checker.getParodies()));
		assertEquals(25, checker.getPages());
	}
}