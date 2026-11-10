package com.budgy.app.data

object DefaultData {

    /** Color swatches offered for accounts, categories and subscriptions. */
    val swatches: List<Long> = listOf(
        0xFF0E6E52, 0xFF12A15F, 0xFF2BA3A0, 0xFF3E8EDE, 0xFF5B6CF0, 0xFF8E6CEF,
        0xFFC77DDB, 0xFFD9578F, 0xFFE0534A, 0xFFF28C38, 0xFFE5A50A, 0xFF8A948F,
    )

    fun categories(): List<Category> {
        val expense = listOf(
            Triple("Food & Dining", "restaurant", 0xFFF28C38),
            Triple("Groceries", "groceries", 0xFF12A15F),
            Triple("Transport", "commute", 0xFF3E8EDE),
            Triple("Bills & Utilities", "bolt", 0xFFE5A50A),
            Triple("Shopping", "shopping", 0xFFD9578F),
            Triple("Health", "health", 0xFFE0534A),
            Triple("Entertainment", "fun", 0xFF8E6CEF),
            Triple("Subscriptions", "subscriptions", 0xFF5B6CF0),
            Triple("Education", "education", 0xFF2BA3A0),
            Triple("Personal Care", "beauty", 0xFFC77DDB),
            Triple("Gifts", "gift", 0xFFE0534A),
            Triple("Other", "other", 0xFF8A948F),
        )
        val income = listOf(
            Triple("Salary", "salary", 0xFF12A15F),
            Triple("Allowance", "allowance", 0xFF2BA3A0),
            Triple("Business", "business", 0xFFE5A50A),
            Triple("Gifts Received", "gift", 0xFFD9578F),
            Triple("Other Income", "invest", 0xFF5B6CF0),
        )
        return expense.mapIndexed { i, (n, ic, c) ->
            Category(name = n, kind = CategoryKind.EXPENSE, icon = ic, color = c, sortOrder = i)
        } + income.mapIndexed { i, (n, ic, c) ->
            Category(name = n, kind = CategoryKind.INCOME, icon = ic, color = c, sortOrder = i)
        }
    }
}
