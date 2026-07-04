package com.daftar.app.kernel.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.daftar.app.kernel.format.ArabicNumbers
import java.util.Locale

// D41: centralized strings with a dev-phase language toggle. The shipped app is
// Arabic-only (D8) — this object is the single place the flip happens.
object Str {
    var arabic by mutableStateOf(false)

    private fun s(en: String, ar: String) = if (arabic) ar else en

    fun money(amount: Long): String =
        if (arabic) ArabicNumbers.format(amount)
        else String.format(Locale.US, "%,d", amount)

    fun count(n: Int): String = money(n.toLong())

    val appToggle get() = s("ع", "EN")

    val tabToday get() = s("Today", "اليوم")
    val tabCustomers get() = s("Customers", "الزبائن")
    val tabReminders get() = s("Reminders", "المواعيد")
    val tabAccount get() = s("Account", "الحساب")

    val todayBook get() = s("Today's book", "دفتر اليوم")
    val receivedToday get() = s("Received today", "مقبوضات اليوم")
    val emptyDay get() = s("Empty day — record the first sale", "صفحة اليوم فارغة — سجّلي أول بيع")
    val sale get() = s("Sale", "بيع")
    val payment get() = s("Payment", "دفعة")
    val oldDebt get() = s("Old debt", "دين قديم")
    val unspecified get() = s("Unspecified", "غير محدد")
    val undo get() = s("Undo", "تراجع")
    val saleSaved get() = s("Sale recorded", "تم تسجيل البيع")
    val paymentSaved get() = s("Payment recorded", "تم تسجيل الدفعة")
    val paidShort get() = s("paid", "دفع")
    val reminders_soon get() = s("Reminders — coming soon", "المواعيد — قريباً")

    val newPayment get() = s("New payment", "دفعة جديدة")
    val amount get() = s("Amount", "المبلغ")
    val forWhat get() = s("For what? (optional)", "عن ماذا؟ (اختياري)")
    val fromWho get() = s("From whom? (optional)", "من؟ (اختياري)")
    val savePayment get() = s("Save payment", "حفظ الدفعة")
    val sourceApprox get() = s("Source ≈", "المصدر تقريباً:")

    val basketHint get() = s("Tap a type to add it to the basket", "اضغطي على صنف لإضافته للسلة")
    val addTypeChip get() = s("+ type", "+ صنف")
    val customerOptional get() = s("Customer (optional)", "الزبون (اختياري)")
    val paidNow get() = s("Paid now", "المدفوع الآن")
    val total get() = s("Total", "الإجمالي")
    val remainderDebt get() = s("Remainder as debt", "الباقي دين")
    val saveSale get() = s("Save sale", "حفظ البيع")
    val unitPrice get() = s("Unit price", "سعر القطعة")
    val close get() = s("Close", "إغلاق")
    val delete get() = s("Delete", "حذف")

    val newType get() = s("New type", "صنف جديد")
    val editType get() = s("Edit type", "تعديل الصنف")
    val name get() = s("Name", "الاسم")
    val basePrice get() = s("Base price", "السعر الأساسي")
    val save get() = s("Save", "حفظ")
    val cancel get() = s("Cancel", "إلغاء")

    val noCustomers get() = s("No customers yet — add the first one", "لا زبائن بعد — أضيفي أول زبون")
    val newCustomer get() = s("New customer", "زبون جديد")
    val fromContacts get() = s("From contacts", "من جهات الاتصال")
    val phoneOptional get() = s("Phone (optional)", "الهاتف (اختياري)")
    val oldDebtOptional get() = s("Old debt (optional)", "دين قديم (اختياري)")
    val owesShop get() = s("owes", "عليها")
    val shopOwes get() = s("for her", "لها")

    val sources get() = s("Sources", "المصادر")
    val typesTitle get() = s("Types", "الأصناف")
    val newSource get() = s("New source", "مصدر جديد")
    val storeClothes get() = s("Store clothes", "بضاعة المحل")
    val addStoreClothes get() = s("+ store clothes", "+ بضاعة المحل")
    val bale get() = s("Bale", "بالة")
    val market get() = s("Market pickings", "شراء من السوق")
    val sourceLabel get() = s("Label (Feb bale…)", "الاسم (بالة شباط…)")
    val costUsdRequired get() = s("Cost in USD (required)", "التكلفة بالدولار (إلزامي)")
    val costUsdOptional get() = s("Cost in USD (optional)", "التكلفة بالدولار (اختياري)")
    val costLocalOptional get() = s("Local equivalent (optional)", "المعادل المحلي (اختياري)")
    val countingSession get() = s("Counting session — new line", "جلسة عدّ — سطر جديد")
    val addCounting get() = s("+ counting session", "+ جلسة عدّ")
    val qtyApprox get() = s("Quantity (approx.)", "الكمية (تقريباً)")
    val pricePoint get() = s("Price point", "نقطة السعر")
    val unitCostOptional get() = s("Unit cost (optional)", "كلفة القطعة (اختياري)")
    val salePricePerUnit get() = s("Sale price per unit", "سعر البيع للقطعة")
    val buyPriceOptional get() = s("Buy price per unit (optional)", "سعر الشراء للقطعة (اختياري)")
    val addStoreTitle get() = s("Store clothes — add", "بضاعة المحل — إضافة")
    val notCounted get() = s("Not counted yet — counting is optional and never blocks selling", "لم تُعدّ بعد — العدّ اختياري ولا يوقف البيع")
    val noSources get() = s("No sources yet — add a bale or the store clothes", "لا مصادر بعد — سجّلي بالة أو بضاعة المحل")
    val noTypes get() = s("No types yet — add the first one", "لا أصناف بعد — أضيفي أول صنف")
    val noTypesHint get() = s("No types yet — add one from the sale screen (+ type)", "لا أصناف بعد — أضيفي صنفاً من شاشة البيع (+ صنف)")
    val remaining get() = s("left ~", "متبقٍ ~")
    val editPrice get() = s("Edit price", "تعديل السعر")
    val addToSource get() = s("+ add to source", "+ إضافة لمصدر")
    val addToSourceTitle get() = s("add to source", "إضافة لمصدر")
    val noSourceLink get() = s("Not linked to a source — sells as unspecified", "غير مرتبط بمصدر — يُباع ويُنسب غير محدد")
    val inStoreNow get() = s("In store now", "الموجود حالياً")
    val cost get() = s("Cost", "التكلفة")
}
