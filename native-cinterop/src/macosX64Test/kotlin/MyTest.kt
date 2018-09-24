import kotlin.test.*

class MyTest {
    @Test
    fun test() {
        assertEquals(7, example.cinterop.stdio.demo())
    }
}