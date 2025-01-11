/**
 * This is the source code of Cherrygram for Android.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 * Please, be respectful and credit the original author if you use this code.
 *
 * Copyright github.com/arsLan4k1390, 2022-2025.
 */

package uz.unnarsx.cherrygram.chats.helpers

import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ChatObject
import org.telegram.messenger.LocaleController.getString
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.ActionBarPopupWindow
import org.telegram.ui.AvatarPreviewer
import org.telegram.ui.Cells.ChatMessageCell
import org.telegram.ui.ChatActivity
import org.telegram.ui.ChatRightsEditActivity
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.ItemOptions
import org.telegram.ui.Components.ShareAlert
import org.telegram.ui.Components.TranslateAlert2
import org.telegram.ui.Components.UndoView
import uz.unnarsx.cherrygram.chats.JsonBottomSheet
import uz.unnarsx.cherrygram.core.configs.CherrygramChatsConfig
import uz.unnarsx.cherrygram.core.configs.CherrygramCoreConfig
import uz.unnarsx.cherrygram.core.configs.CherrygramExperimentalConfig
import uz.unnarsx.cherrygram.core.helpers.CGResourcesHelper
import uz.unnarsx.cherrygram.core.helpers.LocalVerificationsHelper
import uz.unnarsx.cherrygram.helpers.ui.ActionBarPopupWindowHelper

object ChatsHelper2 {

    /** Avatar admin actions start **/
    @JvmStatic
    fun injectChatActivityAvatarArraySize(cf: ChatActivity): Int {
        var objs = 0

        if (ChatObject.canBlockUsers(cf.currentChat)) objs++
        if (ChatObject.hasAdminRights(cf.currentChat)) objs++
        if (ChatObject.canAddAdmins(cf.currentChat)) objs++

        return objs
    }

    @JvmStatic
    fun injectChatActivityAvatarArrayItems(cf: ChatActivity, arr: Array<AvatarPreviewer.MenuItem>, enableMention: Boolean,  enableSearchMessages: Boolean) {
        var startPos = if (enableMention || enableSearchMessages) 3 else 2

        if (ChatObject.canBlockUsers(cf.currentChat)) {
            arr[startPos] = AvatarPreviewer.MenuItem.CG_KICK
            startPos++
        }

        if (ChatObject.hasAdminRights(cf.currentChat)) {
            arr[startPos] = AvatarPreviewer.MenuItem.CG_CHANGE_PERMS
            startPos++
        }

        if (ChatObject.canAddAdmins(cf.currentChat)) {
            arr[startPos] = AvatarPreviewer.MenuItem.CG_CHANGE_ADMIN_PERMS
            startPos++
        }
    }

    @JvmStatic
    fun injectChatActivityAvatarOnClick(cf: ChatActivity, item: AvatarPreviewer.MenuItem, user: TLRPC.User, participantsIDs: ArrayList<Long>) {
        when (item) {
            AvatarPreviewer.MenuItem.CG_KICK -> {
                cf.messagesController.deleteParticipantFromChat(cf.currentChat.id, cf.messagesController.getUser(user.id), cf.currentChatInfo)
            }
            AvatarPreviewer.MenuItem.CG_CHANGE_PERMS, AvatarPreviewer.MenuItem.CG_CHANGE_ADMIN_PERMS -> {
                val action = if (item == AvatarPreviewer.MenuItem.CG_CHANGE_PERMS) 1 else 0 // 0 - change admin rights

                val chatParticipant = cf.currentChatInfo.participants.participants.filter {
                    it.user_id == user.id
                }[0]

                var channelParticipant: TLRPC.ChannelParticipant? = null

                if (ChatObject.isChannel(cf.currentChat)) {
                    channelParticipant = (chatParticipant as TLRPC.TL_chatChannelParticipant).channelParticipant
                } else {
                    chatParticipant is TLRPC.TL_chatParticipantAdmin
                }

                val frag = ChatRightsEditActivity(
                    user.id,
                    cf.currentChatInfo.id,
                    channelParticipant?.admin_rights,
                    cf.currentChat.default_banned_rights,
                    channelParticipant?.banned_rights,
                    channelParticipant?.rank,
                    action,
                    true,
                    false,
                    null
                )

                cf.presentFragment(frag)
            }

            else -> {}
        }
        participantsIDs.clear()
    }

    @JvmStatic
    fun injectChatActivityAvatarOnClickNew(
        chatActivity: ChatActivity, chatMessageCellDelegate: ChatActivity.ChatMessageCellDelegate, cell: ChatMessageCell, user: TLRPC.User,
        enableMention: Boolean, enableSearchMessages: Boolean, isChatParticipant: Boolean,
        participantsIDs: ArrayList<Long>
    ) {
        ItemOptions.makeOptions(chatActivity, cell)
            /*.add(R.drawable.msg_openprofile, getString(R.string.OpenProfile)) {
                chatMessageCellDelegate.openProfile(user)
            }*/
            .add(R.drawable.msg_discussion, getString(R.string.SendMessage)) {
                chatMessageCellDelegate.openDialog(cell, user)
            }
            .addIf(enableMention, R.drawable.msg_mention, getString(R.string.Mention)) {
                chatMessageCellDelegate.appendMention(user)
            }
            .addIf(enableSearchMessages, R.drawable.msg_search, getString(R.string.AvatarPreviewSearchMessages)) {
                chatActivity.openSearchWithUser(user)
            }
            .addIf(ChatObject.canBlockUsers(chatActivity.currentChat) && isChatParticipant, R.drawable.msg_remove, getString(R.string.KickFromGroup)) {
                chatActivity.messagesController.deleteParticipantFromChat(
                    chatActivity.currentChat.id,
                    chatActivity.messagesController.getUser(user.id),
                    chatActivity.currentChatInfo
                )
            }
            .addIf(ChatObject.hasAdminRights(chatActivity.currentChat) && isChatParticipant, R.drawable.msg_permissions, getString(R.string.ChangePermissions)) {
                val action = 1 // Change permissions

                val chatParticipant = chatActivity.currentChatInfo.participants.participants.filter {
                    it.user_id == user.id
                }[0]

                var channelParticipant: TLRPC.ChannelParticipant? = null

                if (ChatObject.isChannel(chatActivity.currentChat)) {
                    channelParticipant = (chatParticipant as TLRPC.TL_chatChannelParticipant).channelParticipant
                } else {
                    chatParticipant is TLRPC.TL_chatParticipantAdmin
                }

                val frag = ChatRightsEditActivity(
                    user.id,
                    chatActivity.currentChatInfo.id,
                    channelParticipant?.admin_rights,
                    chatActivity.currentChat.default_banned_rights,
                    channelParticipant?.banned_rights,
                    channelParticipant?.rank,
                    action,
                    true,
                    false,
                    null
                )
                chatActivity.presentFragment(frag)
            }
            .addIf(ChatObject.canAddAdmins(chatActivity.currentChat) && isChatParticipant, R.drawable.msg_admins, getString(R.string.EditAdminRights)) {
                val action = 0 // Change admin rights

                val chatParticipant = chatActivity.currentChatInfo.participants.participants.filter {
                    it.user_id == user.id
                }[0]

                var channelParticipant: TLRPC.ChannelParticipant? = null

                if (ChatObject.isChannel(chatActivity.currentChat)) {
                    channelParticipant = (chatParticipant as TLRPC.TL_chatChannelParticipant).channelParticipant
                } else {
                    chatParticipant is TLRPC.TL_chatParticipantAdmin
                }

                val frag = ChatRightsEditActivity(
                    user.id,
                    chatActivity.currentChatInfo.id,
                    channelParticipant?.admin_rights,
                    chatActivity.currentChat.default_banned_rights,
                    channelParticipant?.banned_rights,
                    channelParticipant?.rank,
                    action,
                    true,
                    false,
                    null
                )
                chatActivity.presentFragment(frag)
            }
            .setDrawScrim(false)
            .setGravity(Gravity.LEFT)
            .forceBottom(true)
            .translate(0f, -AndroidUtilities.dp(48f).toFloat())
            .show()
        participantsIDs.clear()
    }

    @JvmStatic
    fun getActiveUsername(userId: Long): String {
        val user: TLRPC.User = MessagesController.getInstance(UserConfig.selectedAccount).getUser(userId)
        var username: String? = null
        var usernames = ArrayList<TLRPC.TL_username?>()
        usernames.addAll(user.usernames)
        if (!TextUtils.isEmpty(user.username)) {
            username = user.username
        }
        usernames = ArrayList(user.usernames)
        if (TextUtils.isEmpty(username)) {
            for (i in usernames.indices) {
                val u: TLRPC.TL_username? = usernames[i]
                if (u != null && u.active && !TextUtils.isEmpty(u.username)) {
                    username = u.username
                    break
                }
            }
        }
        return username ?: ""
    }
    /** Avatar admin actions finish **/

    /** Chat search filter start **/
    @JvmStatic
    fun getSearchFilterType(): TLRPC.MessagesFilter {
        val filter: TLRPC.MessagesFilter = when (CherrygramChatsConfig.messagesSearchFilter) {
            CherrygramChatsConfig.FILTER_PHOTOS -> {
                TLRPC.TL_inputMessagesFilterPhotos()
            }
            CherrygramChatsConfig.FILTER_VIDEOS -> {
                TLRPC.TL_inputMessagesFilterVideo()
            }
            CherrygramChatsConfig.FILTER_VOICE_MESSAGES -> {
                TLRPC.TL_inputMessagesFilterVoice()
            }
            CherrygramChatsConfig.FILTER_VIDEO_MESSAGES -> {
                TLRPC.TL_inputMessagesFilterRoundVideo()
            }
            CherrygramChatsConfig.FILTER_FILES -> {
                TLRPC.TL_inputMessagesFilterDocument()
            }
            CherrygramChatsConfig.FILTER_MUSIC -> {
                TLRPC.TL_inputMessagesFilterMusic()
            }
            CherrygramChatsConfig.FILTER_GIFS -> {
                TLRPC.TL_inputMessagesFilterGif()
            }
            CherrygramChatsConfig.FILTER_GEO -> {
                TLRPC.TL_inputMessagesFilterGeo()
            }
            CherrygramChatsConfig.FILTER_CONTACTS -> {
                TLRPC.TL_inputMessagesFilterContacts()
            }
            CherrygramChatsConfig.FILTER_MENTIONS -> {
                TLRPC.TL_inputMessagesFilterMyMentions()
            }
            else -> {
                TLRPC.TL_inputMessagesFilterEmpty()
            }
        }
        return filter
    }
    /** Chat search filter finish **/

    /** Custom chat id for Saved Messages start **/
    @JvmStatic
    fun checkCustomChatID(currentAccount: Int) {
        val preferences = MessagesController.getMainSettings(currentAccount)
        val empty = preferences.getString("CP_CustomChatIDSM", "CP_CustomChatIDSM").equals("")
        if (empty) {
            CherrygramCoreConfig.putStringForUserPrefs("CP_CustomChatIDSM",
                UserConfig.getInstance(currentAccount).getClientUserId().toString()
            )
//            FileLog.e("changed the id")
        }
    }

    @JvmStatic
    fun getCustomChatID(): Long {
        val id: Long
        val preferences = MessagesController.getMainSettings(UserConfig.selectedAccount)
        val savedMessagesChatID =
            preferences.getString("CP_CustomChatIDSM", UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId().toString())
        val chatID = savedMessagesChatID!!.replace("-100", "-").toLong()

        id = if (CherrygramExperimentalConfig.customChatForSavedMessages) {
            chatID
        } else {
            UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId()
        }
        return id
    }
    /** Custom chat id for Saved Messages finish **/

    /** Direct share menu start **/
    private var currentPopup: ActionBarPopupWindow? = null
    @JvmStatic //TODO Move to ItemOptions.java
    fun showForwardMenu(sa: ShareAlert, field: FrameLayout) {
        currentPopup = ActionBarPopupWindowHelper.createPopupWindow(sa.container, field, sa.context, listOf(
            ActionBarPopupWindowHelper.PopupItem(
                if (CherrygramChatsConfig.forwardNoAuthorship)
                    getString(R.string.CG_FwdMenu_DisableNoForward)
                else getString(R.string.CG_FwdMenu_EnableNoForward),
                R.drawable.msg_forward
            ) {
                // Toggle!
                CherrygramChatsConfig.forwardNoAuthorship = !CherrygramChatsConfig.forwardNoAuthorship
                currentPopup?.dismiss()
                currentPopup = null
            },
            ActionBarPopupWindowHelper.PopupItem(
                if (CherrygramChatsConfig.forwardWithoutCaptions)
                    getString(R.string.CG_FwdMenu_EnableCaptions)
                else getString(R.string.CG_FwdMenu_DisableCaptions),
                R.drawable.msg_edit
            ) {
                // Toggle!
                CherrygramChatsConfig.forwardWithoutCaptions = !CherrygramChatsConfig.forwardWithoutCaptions
                currentPopup?.dismiss()
                currentPopup = null
            },
            ActionBarPopupWindowHelper.PopupItem(
                if (CherrygramChatsConfig.forwardNotify)
                    getString(R.string.CG_FwdMenu_NoNotify)
                else getString(R.string.CG_FwdMenu_Notify),
                R.drawable.input_notify_on
            ) {
                // Toggle!
                CherrygramChatsConfig.forwardNotify = !CherrygramChatsConfig.forwardNotify
                currentPopup?.dismiss()
                currentPopup = null
            },
        ))
    }
    /** Direct share menu finish **/

    /** JSON menu start **/
    @JvmStatic //TODO Move to ItemOptions.java
    fun showJsonMenu(sa: JsonBottomSheet, field: FrameLayout, messageObject: MessageObject) {
        currentPopup = ActionBarPopupWindowHelper.createPopupWindow(sa.container, field, sa.context, listOf(
            ActionBarPopupWindowHelper.PopupItem(
                if (sa.isJacksonSupportedAndEnabled) "Switch to GSON" else "Switch to Jackson",
                R.drawable.msg_info
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CherrygramChatsConfig.jacksonJSON_Provider = !CherrygramChatsConfig.jacksonJSON_Provider
                    sa.dismiss()
                    JsonBottomSheet.showAlert(sa.context, sa.fragment, messageObject, null)
                } else {
                    BulletinFactory.of(sa.container, null)
                        .createSimpleBulletin(R.raw.error, "Jackson library is supported on Android 8 and newer.")
                        .show()
                }
                currentPopup?.dismiss()
                currentPopup = null
            },
            ActionBarPopupWindowHelper.PopupItem(
                "Date: " + CGResourcesHelper.createDateAndTimeForJSON(messageObject.messageOwner.date.toLong()),
                R.drawable.msg_calendar2
            ) {
                AndroidUtilities.addToClipboard(CGResourcesHelper.createDateAndTimeForJSON(messageObject.messageOwner.date.toLong()))
                BulletinFactory.of(field, null).createCopyBulletin(getString(R.string.TextCopied)).show()
                currentPopup?.dismiss()
                currentPopup = null
            }
        ))
    }
    /** JSON menu finish **/

    /** Message slide action start **/
    @JvmStatic
    fun injectChatActivityMsgSlideAction(cf: ChatActivity, msg: MessageObject, isChannel: Boolean, classGuid: Int) {
        when (CherrygramChatsConfig.messageSlideAction) {
            CherrygramChatsConfig.MESSAGE_SLIDE_ACTION_REPLY -> {
                // Reply (default)
                cf.showFieldPanelForReply(msg)
            }
            CherrygramChatsConfig.MESSAGE_SLIDE_ACTION_SAVE -> {
                // Save message
                val id = getCustomChatID()

                cf.sendMessagesHelper.sendMessage(arrayListOf(msg), id, false, false, true, 0)

                cf.createUndoView()
                if (cf.undoView == null) {
                    return
                }
                if (!CherrygramExperimentalConfig.customChatForSavedMessages) {
                    if (!BulletinFactory.of(cf).showForwardedBulletinWithTag(id, arrayListOf(msg).size)) {
                        cf.undoView!!.showWithAction(id, UndoView.ACTION_FWD_MESSAGES, arrayListOf(msg).size)
                    }
                } else {
                    cf.undoView!!.showWithAction(id, UndoView.ACTION_FWD_MESSAGES, arrayListOf(msg).size)
                }
            }
            CherrygramChatsConfig.MESSAGE_SLIDE_ACTION_TRANSLATE -> {
                // Translate
                val languageAndTextToTranslate: String = msg.messageOwner.message
                val toLang = TranslateAlert2.getToLanguage()
                val alert = TranslateAlert2.showAlert(
                    cf.context,
                    cf,
                    UserConfig.selectedAccount,
                    languageAndTextToTranslate,
                    toLang,
                    languageAndTextToTranslate,
                    null,
                    false,
                    null
                ) { cf.dimBehindView(false) }
                alert.setDimBehindAlpha(100)
                alert.setDimBehind(true)
            }
            CherrygramChatsConfig.MESSAGE_SLIDE_ACTION_DIRECT_SHARE -> {
                // Direct Share
                cf.showDialog(object : ShareAlert(cf.parentActivity, arrayListOf(msg), null, isChannel, null, false) {
                    override fun dismissInternal() {
                        super.dismissInternal()
                        AndroidUtilities.requestAdjustResize(cf.parentActivity, classGuid)
                        if (cf.chatActivityEnterView.visibility == View.VISIBLE) {
                            cf.fragmentView.requestLayout()
                        }
                        cf.updatePinnedMessageView(true)
                    }
                })

                AndroidUtilities.setAdjustResizeToNothing(cf.parentActivity, classGuid)
                cf.fragmentView.requestLayout()
            }
        }
    }
    /** Message slide action finish **/

    /*fun isCherryVerified(chat: TLRPC.Chat): Boolean {
        return LocalVerificationsHelper.getVerify().stream().anyMatch { id: Long -> id == chat.id }
    }*/

    fun isDeleteAllHidden(chat: TLRPC.Chat): Boolean {
        return LocalVerificationsHelper.hideDeleteAll().stream().anyMatch { id: Long -> id == chat.id }
    }

}