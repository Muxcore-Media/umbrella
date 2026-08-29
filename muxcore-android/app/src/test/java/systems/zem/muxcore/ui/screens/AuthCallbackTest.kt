package systems.zem.muxcore.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCallbackTest {

  @Test
  fun `authCallbackCode extracts mux BFF callback`() {
    val code = authCallbackCode(
      "https://mux.zem.systems/auth/callback?code=abc123",
      "https://mux.zem.systems",
    )
    assertEquals("abc123", code)
  }

  @Test
  fun `authCallbackCode extracts mobile deep link`() {
    val code = authCallbackCode("muxcore://auth/callback?code=mobile1", "https://mux.zem.systems")
    assertEquals("mobile1", code)
  }

  @Test
  fun `authCallbackCode ignores auth host`() {
    assertNull(
      authCallbackCode(
        "https://auth.zem.systems/login?redirect=...",
        "https://mux.zem.systems",
      ),
    )
  }

  @Test
  fun `authCallbackCode ignores wrong server host`() {
    assertNull(
      authCallbackCode(
        "https://other.example/auth/callback?code=x",
        "https://mux.zem.systems",
      ),
    )
  }

  @Test
  fun `sessionCookieFrom parses Cookie header`() {
    assertEquals("tok", sessionCookieFrom("session=tok; Path=/"))
    assertNull(sessionCookieFrom("other=1"))
  }

  @Test
  fun `isMuxcoreAuthDeepLink`() {
    assertTrue(isMuxcoreAuthDeepLink("muxcore://auth/callback?code=1"))
    assertFalse(isMuxcoreAuthDeepLink("https://mux.zem.systems/auth/callback?code=1"))
  }
}
