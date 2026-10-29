package com.mohitarora.dualclone.domain

import com.mohitarora.dualclone.core.common.UiText
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.ui.viewmodel.Messages
import org.junit.Assert.assertEquals
import org.junit.Test

class MessagesTest {

    private fun p(kind: ProfileKind, name: String) = Profile(userId = 10, name = name, kind = kind)

    private fun clonedText(profile: Profile) =
        (Messages.cloned("WhatsApp", Messages.profileLabel(profile)) as UiText.Plain).value

    @Test
    fun workProfileIsNeverCalledByItsSystemName() {
        assertEquals("Cloned WhatsApp to Work profile", clonedText(p(ProfileKind.WORK, "Island")))
    }

    @Test
    fun labelsMatchTheChipTextPerKind() {
        assertEquals("Dual Messenger", Messages.profileLabel(p(ProfileKind.DUAL_MESSENGER, "DUAL_APP")))
        assertEquals("Work profile", Messages.profileLabel(p(ProfileKind.WORK, "Island")))
        assertEquals("Secure Folder", Messages.profileLabel(p(ProfileKind.SECURE_FOLDER, "Secure Folder")))
        assertEquals("Primary profile", Messages.profileLabel(p(ProfileKind.PRIMARY, "Owner")))
    }

    @Test
    fun otherUserUsesTheUserNameWithAFallback() {
        assertEquals("Anu Arora", Messages.profileLabel(p(ProfileKind.OTHER_USER, "Anu Arora")))
        assertEquals("Other user", Messages.profileLabel(p(ProfileKind.OTHER_USER, "  ")))
    }

    @Test
    fun pausedWarningUsesTheKindLabel() {
        val text = (Messages.profilePaused(Messages.profileLabel(p(ProfileKind.WORK, "Island"))) as UiText.Plain).value
        assertEquals("Work profile is paused. Resume it before cloning or removing apps.", text)
    }

    @Test
    fun removedWithRefreshFormat() {
        val text = (Messages.removedWithRefresh("WhatsApp") as UiText.Plain).value
        assertEquals("WhatsApp removed. Home screen refreshed", text)
    }

    @Test
    fun refreshLauncherActionLabel() {
        val text = (Messages.refreshLauncherAction() as UiText.Plain).value
        assertEquals("Refresh launcher", text)
    }
}
