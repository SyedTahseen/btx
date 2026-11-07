package com.betterx.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val LAST_UPDATED = "Last updated: November 2026"

private data class Faq(val q: String, val a: String)
private data class LegalSection(val title: String, val body: String)

// ============================================================================ content

private val faqs = listOf(
    Faq(
        "What is BetterX?",
        "BetterX is a private education and coaching app for sexual wellness. It gives you a daily plan, short lessons, practices, check-ins and a coach to talk things through with.",
    ),
    Faq(
        "Is BetterX medical advice?",
        "No. BetterX offers education and coaching only, not medical care. If you have pain, a medical concern or a mental health concern, please speak with a qualified doctor or therapist.",
    ),
    Faq(
        "Who can use BetterX?",
        "BetterX is for adults. You must be 18 or older to use it.",
    ),
    Faq(
        "Where is my data stored?",
        "Your plan, check-ins, answers and coach chats are stored on your device in encrypted form. Your email and basic profile (name, relationship, goals and the optional \"About you\") are saved with your account online so you can restore them when you sign in again. Anything you post in Ghost is stored online so other members can read it.",
    ),
    Faq(
        "How is my privacy protected?",
        "Screenshots and screen recording are blocked inside the app, and the app is excluded from device backups. You can also turn on Discreet notifications in Settings so nothing sensitive appears on your lock screen.",
    ),
    Faq(
        "How does the coach work?",
        "The coach answers using your plan, goals and progress to give tailored guidance. It is not a doctor or therapist. If you tell it about something that sounds urgent or unsafe, it will point you to appropriate help instead of continuing as normal.",
    ),
    Faq(
        "What is the difference between Free and Pro?",
        "Free includes your daily plan, check-ins, progress tracking, reminders, the Ghost community and 5 coach messages a day. Pro adds unlimited coaching, every lesson in every course, your full week-ahead plan and early access to new programs. Open Settings > Subscription to compare the plans.",
    ),
    Faq(
        "What is Ghost?",
        "Ghost is an anonymous community where members share experiences, reply to each other and show support with a heart. Every post and reply gets a random alias, never your name or email. You can delete your own posts and replies at any time, and report anything that breaks the guidelines.",
    ),
    Faq(
        "How do I change my name, goals or relationship status?",
        "Open Settings and tap your profile card at the top. Change what you like and tap Save changes.",
    ),
    Faq(
        "My reminders are not showing up. What can I do?",
        "Check that the reminder is switched on in Settings and that notifications are allowed for BetterX in your phone's system settings. Some phones also limit background activity for apps to save battery, so it can help to remove that limit for BetterX.",
    ),
    Faq(
        "I forgot my password.",
        "On the Log in screen tap \"Forgot password?\" and enter your email. We will send you a link to choose a new password. If you are signed in, you can change it any time in Settings > Change password.",
    ),
    Faq(
        "What is the difference between Log out and Delete account?",
        "Log out keeps your data on this device so it is here when you sign back in. Delete account permanently removes your account, your online profile and everything stored on this device. This cannot be undone.",
    ),
)

private val termsSections = listOf(
    LegalSection(
        "1. Agreeing to these terms",
        "By creating an account or using BetterX you agree to these Terms of Service. If you do not agree, please do not use the app.",
    ),
    LegalSection(
        "2. Who can use BetterX",
        "BetterX is for adults only. You must be at least 18 years old, or the age of majority where you live if that is higher. By using the app you confirm that you meet this requirement.",
    ),
    LegalSection(
        "3. Not medical care",
        "BetterX provides education and coaching only. Nothing in the app, including lessons, practices, scores and coach replies, is medical advice, diagnosis or treatment, and it does not replace a qualified healthcare professional. Always seek professional help for medical or mental health concerns. In an emergency, contact your local emergency services.",
    ),
    LegalSection(
        "4. Your account",
        "You are responsible for the details you give us, for keeping your password private and for activity on your account. Tell us if you think someone else has access to it. You can change your password or delete your account at any time in Settings.",
    ),
    LegalSection(
        "5. Acceptable use",
        "When you use BetterX you agree not to:\n• break the law or use the app to harm others\n• post content that is abusive, hateful, sexually explicit involving minors, or that reveals someone else's private information\n• try to access other people's data, interfere with the app or its security, or reverse engineer it\n• copy, resell or republish our content without permission",
    ),
    LegalSection(
        "6. Subscriptions",
        "BetterX has a Free plan and a paid Pro plan. When Pro purchases become available they will be handled through Google Play. Pro renews automatically until you cancel it in Google Play, and you can cancel at any time. Prices, features and plans may change, and we will tell you before a change affects a subscription you already have.",
    ),
    LegalSection(
        "7. Content you share",
        "You keep ownership of what you write, including anything you share in Ghost. Ghost posts and replies are visible to other members under a random alias. You give us permission to store and display your content as needed to run the app. Please do not share personal details such as your full name, address or phone number. We may remove content that breaks these terms or the community guidelines.",
    ),
    LegalSection(
        "8. Our content",
        "The lessons, plans, designs, text and other material in BetterX belong to us or our licensors and are protected by law. You may use them for your own personal use inside the app.",
    ),
    LegalSection(
        "9. Availability and changes",
        "We work to keep BetterX running well, but we cannot promise it will always be available or error-free. We may add, change or remove features, and we may suspend access if these terms are broken.",
    ),
    LegalSection(
        "10. Limits of responsibility",
        "BetterX is provided \"as is\". To the extent the law allows, we are not responsible for losses that result from using, or being unable to use, the app, or from relying on its guidance in place of professional advice. Nothing in these terms limits any rights you have under the law that cannot be limited.",
    ),
    LegalSection(
        "11. Changes to these terms",
        "We may update these terms from time to time. If a change is important we will let you know in the app. Continuing to use BetterX after a change means you accept the updated terms.",
    ),
    LegalSection(
        "12. Contact",
        "Questions about these terms? Contact the BetterX team through the support details on our Google Play page.",
    ),
)

private val privacySections = listOf(
    LegalSection(
        "Our approach",
        "BetterX is built to be private. We collect only what is needed to run the app, we do not sell your data, and we do not show ads.",
    ),
    LegalSection(
        "What we collect",
        "Account: your email address and password, handled by Google Firebase Authentication.\n\nOnline profile: your name, relationship status, the optional \"About you\" text and your goals, saved with your account so you can restore them on a new device.\n\nGhost: the posts, replies and hearts you share. They are stored online so other members can see them, under a random alias. They are stored with an internal account ID, which the app does not show to other members, so you can manage and delete your own content.\n\nOn your device only: your plan, check-ins, quiz answers and scores, coach conversations, reminder settings and your Free or Pro status.",
    ),
    LegalSection(
        "How we use it",
        "We use your information to sign you in, personalise your plan and coaching, send the reminders you ask for, and keep the app working and secure.",
    ),
    LegalSection(
        "How it is protected",
        "Data stored on your device is encrypted. The app blocks screenshots and screen recording, and it is excluded from device backups. Online data is stored with Google Firebase services and sent over secure connections. No system is perfectly secure, but we take care to protect your information.",
    ),
    LegalSection(
        "The coach",
        "By default the coach runs on your device and your conversations stay there. If your version of BetterX is connected to an online coach service, your recent messages and basic context (such as your name, focus areas, streak and scores) are sent to that service to write a reply. Safety replies are always handled on your device.",
    ),
    LegalSection(
        "Notifications",
        "BetterX asks for permission to send reminders. You can turn them off in Settings or in your phone's system settings. With Discreet notifications on, reminders hide sensitive details on your lock screen.",
    ),
    LegalSection(
        "Sharing",
        "We do not sell or rent your personal information. We share it only with the service providers that run the app for us, such as Google Firebase and, when purchases are available, Google Play, or when the law requires it. Anything you post in Ghost is visible to other BetterX members.",
    ),
    LegalSection(
        "Keeping and deleting your data",
        "Your online profile is kept while your account exists. You can delete your own Ghost posts and replies at any time. When you choose Delete account in Settings, we delete your account, your online profile and your Ghost posts, and clear everything stored on your device. Replies you wrote on other members' posts may remain under their random alias. Logging out does not delete your data on this device.",
    ),
    LegalSection(
        "Your choices",
        "You can view and change your profile in Settings, change or reset your password, turn reminders off, log out, or delete your account at any time.",
    ),
    LegalSection(
        "Adults only",
        "BetterX is not intended for anyone under 18, and we do not knowingly collect information from minors.",
    ),
    LegalSection(
        "Changes to this policy",
        "We may update this policy from time to time. If a change is important we will tell you in the app.",
    ),
    LegalSection(
        "Contact",
        "Questions about your privacy? Contact the BetterX team through the support details on our Google Play page.",
    ),
)

// ============================================================================ screens

@Composable
fun FaqScreen(onBack: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var open by remember { mutableStateOf<Int?>(null) }
    ScrollPage("FAQ", onBack) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(BX.Card)
                .padding(vertical = 4.dp),
        ) {
            faqs.forEachIndexed { i, f ->
                val isOpen = open == i
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClickLabel = if (isOpen) "Collapse" else "Expand") {
                            open = if (isOpen) null else i
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.q, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        Icon(
                            if (isOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = muted,
                            modifier = Modifier.padding(start = 8.dp).size(22.dp),
                        )
                    }
                    if (isOpen) {
                        Text(f.a, style = MaterialTheme.typography.bodyMedium, color = muted, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                if (i < faqs.lastIndex) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(BX.CardBorder))
                }
            }
        }
        Text(
            "Can't find your answer? Contact the BetterX team through the support details on our Google Play page.",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun LegalPage(title: String, sections: List<LegalSection>, onBack: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    ScrollPage(title, onBack) {
        Text(LAST_UPDATED, style = MaterialTheme.typography.bodySmall, color = muted, modifier = Modifier.padding(start = 6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(BX.Card)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            sections.forEach { s ->
                Column {
                    Text(s.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        s.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD0D1D5),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun TermsScreen(onBack: () -> Unit) = LegalPage("Terms of Service", termsSections, onBack)

@Composable
fun PrivacyScreen(onBack: () -> Unit) = LegalPage("Privacy Policy", privacySections, onBack)
