package model


class RegexPatterns {
    companion object{
        val spacingRegex = Regex("(([0-9](?=([^0-9(,\\s.])))|([a-zA-Z](?=[^a-zA-Z(,\\s]))|([(,)+\\-*/$%^](?=\\S)))")
        val moreThanOneSpaces = Regex("\\s{2,}")
        val referenceRegex = Regex("@\\(\\s*(?<LOCAL>\\.\\.)?(?<PATH>[a-zA-Z0-9/_-]+)\\s*\\)")
        val operatorRegex = Regex("[-+/*%^u]")
        val functionRegex = Regex("random|roll|ceil|floor|round|min|max")
        val punctuationRegex = Regex("[(,)]")
        val commentRegex = Regex("(?<=\\[\")(.*?)(?=\"])")
        val doubleRegex = Regex("-?[0-9]+\\.[0-9]+")
        val integerRegex = Regex("(?<!\\.[0-9]{0,999})(?!-?[0-9]*\\.+)(-?[0-9]+)")
        val stringRegex = Regex("(\")(?:(?!\\1).|\\.)*\\1")
        val literalRegex = Regex("(\"(.*?)\")|((?<!\\.[0-9]{0,999})(?!-?[0-9]*\\.+)(-?[0-9]+))|(-?[0-9]+\\.[0-9]+)")
        val scannerRegex = Regex("(?<REFERENCE>@\\(\\s*(..)?[a-zA-Z0-9/_-]+\\s*\\))|(?<FUNCTIONS>random|roll|ceil|floor|round|max|min)|(?<OPERATORS>[-/*+%^!u])|(?<PUNCTUATIONS>[(,)])|(?<COMMENTS>\\[\".*?\"\\])|(?<INTEGERS>(?<!\\.[0-9]{0,999})(?!-?[0-9]*\\.+)([0-9]+))|(?<DOUBLES>[0-9]+\\.[0-9]+)")
        val unaryOperatorRegex = Regex("(?<=(^|[-+/*%^(,])\\s{0,999})(-)")
        val nameRegex = Regex("[a-zA-Z0-9_-]*")
        fun getReferenceGroupNameAndAttribute(unparsedText: String): Pair<String, String>{
            val match = referenceRegex.matchEntire(unparsedText) ?: return Pair("","")
            val groupName = match.groupValues[1]
            val attributeName = match.groupValues[2]
            return Pair(groupName, attributeName)
        }
    }
}