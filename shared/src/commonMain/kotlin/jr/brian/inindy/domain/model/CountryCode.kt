package jr.brian.inindy.domain.model

data class CountryCode(
    val isoCode: String,
    val dialCode: String,
    val displayName: String
) {
    companion object {
        val US = CountryCode(isoCode = "US", dialCode = "+1", displayName = "United States")
        val DEFAULT = US
    }
}
