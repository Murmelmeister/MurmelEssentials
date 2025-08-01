package de.murmelmeister.essentials.utils;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import de.murmelmeister.murmelapi.language.message.LocalizedMessage;
import de.murmelmeister.murmelapi.language.message.MessageDefinition;
import de.murmelmeister.murmelapi.language.message.MessageService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static de.murmelmeister.murmelapi.language.message.LocalizedMessage.of;

public enum Messages implements MessageDefinition {
    PLAY_TIME_COMMAND_USE(
            of(getLanguageId(Lang.ENGLISH), "<#999999>PlayTime: <#00cc88>[TIME]"),
            of(getLanguageId(Lang.GERMAN), "<#999999>Spielzeit: <#00cc88>[TIME]")
    ),
    PLAY_TIME_COMMAND_OTHER(
            of(getLanguageId(Lang.ENGLISH), "<#999999>PlayTime from <#999900>[PLAYER]</#999900>: <#00cc88>[TIME]"),
            of(getLanguageId(Lang.GERMAN), "<#999999>Spielzeit von <#999900>[PLAYER]</#999900>: <#00cc88>[TIME]")
    ),
    SEND_PUNISHMENT_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), """
                    <#990000>You are blocked from the network.
                    <#999999>Reason: <#999900>[REASON]
                    <#999999>From: <#999900>[FROM]
                    <#999999>Start: <#999900>[START_TIME]
                    <#999999>Expires: <#999900>[EXPIRES_TIME] [MODIFIED]
                    <#999999>Auto-Punish: <#999900>[AUTO_PUNISH]
                    <#999999>Punishment-ID: <#999900>[PUNISHMENT_ID]"""),
            of(getLanguageId(Lang.GERMAN), """
                    <#990000>Du bist vom Netzwerk gesperrt.
                    <#999999>Grund: <#999900>[REASON]
                    <#999999>Von: <#999900>[FROM]
                    <#999999>Start: <#999900>[START_TIME]
                    <#999999>Läuft ab: <#999900>[EXPIRES_TIME] [MODIFIED]
                    <#999999>Automatische-Sperre: <#999900>[AUTO_PUNISH]
                    <#999999>Sperr-ID: <#999900>[PUNISHMENT_ID]""")
    ),
    MESSAGE_YES(
            of(getLanguageId(Lang.ENGLISH), "<#00cc88>Yes"),
            of(getLanguageId(Lang.GERMAN), "<#00cc88>Ja")
    ),
    MESSAGE_NO(
            of(getLanguageId(Lang.ENGLISH), "<#cc0088>No"),
            of(getLanguageId(Lang.GERMAN), "<#cc0088>Nein")
    ),
    MESSAGE_NOT_EXPIRE(
            of(getLanguageId(Lang.ENGLISH), "Never"),
            of(getLanguageId(Lang.GERMAN), "Nie")
    ),
    MESSAGE_MODIFIED(
            of(getLanguageId(Lang.ENGLISH), "<#999999>(modified)"),
            of(getLanguageId(Lang.GERMAN), "<#999999>(geändert)")
    ),
    COMMAND_ERROR_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Command error: [ERROR]"),
            of(getLanguageId(Lang.GERMAN), "<#990000>Kommand Fehler: [ERROR]")
    ),
    COMMAND_ERROR_NO_EXECUTOR(
            of(getLanguageId(Lang.ENGLISH), "Executor user not found."),
            of(getLanguageId(Lang.GERMAN), "Ausführender Benutzer nicht gefunden.")
    ),
    COMMAND_DEBUG_EXECUTION_TIME_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#999900>Command failed after [EXECUTION_TIME] ms"),
            of(getLanguageId(Lang.GERMAN), "<#999900>Der Kommand ist nach [EXECUTION_TIME] ms fehlgeschlagen")
    ),
    COMMAND_DEBUG_EXECUTION_TIME_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999900>Command executed successfully in [EXECUTION_TIME] ms"),
            of(getLanguageId(Lang.GERMAN), "<#999900>Der Kommand wurde erfolgreich in [EXECUTION_TIME] ms ausgeführt")
    ),
    COMMAND_DEBUG_EXECUTION_ERROR(
            of(getLanguageId(Lang.ENGLISH), "<#990000>An unexpected error occurred while executing the command."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Ein unerwarteter Fehler ist beim Ausführen des Kommandos aufgetreten.")
    ),
    COMMAND_DEBUG_EXECUTION_SUCCESS_ROWS(
            of(getLanguageId(Lang.ENGLISH), "<#999900>Rows affected: [ROWS]"),
            of(getLanguageId(Lang.GERMAN), "<#999900>Betroffene Zeilen: [ROWS]")
    ),
    DATE_TIME_FORMAT(
            of(getLanguageId(Lang.ENGLISH), "dd.MM.yyyy HH:mm:ss"),
            of(getLanguageId(Lang.GERMAN), "dd.MM.yyyy HH:mm:ss")
    ),
    PERMISSION_USER_PERMISSION_EXISTS(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Permission <#999900>[PERMISSION]</#999900> already exists for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Die Berechtigung <#999900>[PERMISSION]</#999900> existiert bereits für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PERMISSION_EXISTS(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Permission <#999900>[PERMISSION]</#999900> already exists for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Die Berechtigung <#999900>[PERMISSION]</#999900> existiert bereits für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PARENT_EXISTS(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Parent <#999900>[PARENT]</#999900> already exists for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Der Elternteil <#999900>[PARENT]</#999900> existiert bereits für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PARENT_EXISTS(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Parent <#999900>[PARENT]</#999900> already exists for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Der Elternteil <#999900>[PARENT]</#999900> existiert bereits für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PERMISSION_NOT_EXISTS(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Permission <#999900>[PERMISSION]</#999900> does not exist for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Die Berechtigung <#999900>[PERMISSION]</#999900> existiert nicht für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PERMISSION_NOT_EXISTS(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Permission <#999900>[PERMISSION]</#999900> does not exist for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Die Berechtigung <#999900>[PERMISSION]</#999900> existiert nicht für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PARENT_NOT_EXISTS(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Parent <#999900>[PARENT]</#999900> does not exist for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Der Elternteil <#999900>[PARENT]</#999900> existiert nicht für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PARENT_NOT_EXISTS(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Parent <#999900>[PARENT]</#999900> does not exist for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Der Elternteil <#999900>[PARENT]</#999900> existiert nicht für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PERMISSION_LIST_EMPTY(
            of(getLanguageId(Lang.ENGLISH), "<#990000>User <#00cc88>[USER_NAME]</#00cc88> has no permissions."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Der Benutzer <#00cc88>[USER_NAME]</#00cc88> hat keine Berechtigungen.")
    ),
    PERMISSION_GROUP_PERMISSION_LIST_EMPTY(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Group <#00cc88>[GROUP_NAME]</#00cc88> has no permissions."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Die Gruppe <#00cc88>[GROUP_NAME]</#00cc88> hat keine Berechtigungen.")
    ),
    PERMISSION_USER_PARENT_LIST_EMPTY(
            of(getLanguageId(Lang.ENGLISH), "<#990000>User <#00cc88>[USER_NAME]</#00cc88> has no parents."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Der Benutzer <#00cc88>[USER_NAME]</#00cc88> hat keine Elternteile.")
    ),
    PERMISSION_GROUP_PARENT_LIST_EMPTY(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Group <#00cc88>[GROUP_NAME]</#00cc88> has no parents."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Die Gruppe <#00cc88>[GROUP_NAME]</#00cc88> hat keine Elternteile.")
    ),
    PERMISSION_USER_PERMISSION_ADD_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to add permission <#999900>[PERMISSION]</#999900> for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Hinzufügen der Berechtigung <#999900>[PERMISSION]</#999900> für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PERMISSION_ADD_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to add permission <#999900>[PERMISSION]</#999900> for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Hinzufügen der Berechtigung <#999900>[PERMISSION]</#999900> für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PARENT_ADD_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to add parent <#999900>[PARENT]</#999900> for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Hinzufügen des Elternteils <#999900>[PARENT]</#999900> für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PARENT_ADD_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to add parent <#999900>[PARENT]</#999900> for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Hinzufügen des Elternteils <#999900>[PARENT]</#999900> für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PERMISSION_ADD_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Permission <#009999>[PERMISSION]</#009999> is now added to user <#00cc88>[USER]</#00cc88>. [EXPIRED]"),
            of(getLanguageId(Lang.GERMAN), "<#999999>Die Berechtigung <#009999>[PERMISSION]</#009999> wurde dem Benutzer <#00cc88>[USER]</#00cc88> hinzugefügt. [EXPIRED]")
    ),
    PERMISSION_GROUP_PERMISSION_ADD_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Permission <#009999>[PERMISSION]</#009999> is now added to group <#00cc88>[GROUP]</#00cc88>. [EXPIRED]"),
            of(getLanguageId(Lang.GERMAN), "<#999999>Die Berechtigung <#009999>[PERMISSION]</#009999> wurde der Gruppe <#00cc88>[GROUP]</#00cc88> hinzugefügt. [EXPIRED]")
    ),
    PERMISSION_USER_PARENT_ADD_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Parent <#009999>[PARENT]</#009999> is now added to user <#00cc88>[USER]</#00cc88>. [EXPIRED]"),
            of(getLanguageId(Lang.GERMAN), "<#999999>Der Elternteil <#009999>[PARENT]</#009999> wurde dem Benutzer <#00cc88>[USER]</#00cc88> hinzugefügt. [EXPIRED]")
    ),
    PERMISSION_GROUP_PARENT_ADD_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Parent <#009999>[PARENT]</#009999> is now added to group <#00cc88>[GROUP]</#00cc88>. [EXPIRED]"),
            of(getLanguageId(Lang.GERMAN), "<#999999>Der Elternteil <#009999>[PARENT]</#009999> wurde der Gruppe <#00cc88>[GROUP]</#00cc88> hinzugefügt. [EXPIRED]")
    ),
    PERMISSION_USER_PERMISSION_REMOVE_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to remove permission <#999900>[PERMISSION]</#999900> for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Entfernen der Berechtigung <#999900>[PERMISSION]</#999900> für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PERMISSION_REMOVE_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to remove permission <#999900>[PERMISSION]</#999900> for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Entfernen der Berechtigung <#999900>[PERMISSION]</#999900> für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PARENT_REMOVE_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to remove parent <#999900>[PARENT]</#999900> for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Entfernen des Elternteils <#999900>[PARENT]</#999900> für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PARENT_REMOVE_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to remove parent <#999900>[PARENT]</#999900> for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Entfernen des Elternteils <#999900>[PARENT]</#999900> für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PERMISSION_REMOVE_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Permission <#009999>[PERMISSION]</#009999> is now removed from user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Die Berechtigung <#009999>[PERMISSION]</#009999> wurde dem Benutzer <#00cc88>[USER]</#00cc88> entfernt.")
    ),
    PERMISSION_GROUP_PERMISSION_REMOVE_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Permission <#009999>[PERMISSION]</#009999> is now removed from group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Die Berechtigung <#009999>[PERMISSION]</#009999> wurde der Gruppe <#00cc88>[GROUP]</#00cc88> entfernt.")
    ),
    PERMISSION_USER_PARENT_REMOVE_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Parent <#009999>[PARENT]</#009999> is now removed from user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Der Elternteil <#009999>[PARENT]</#009999> wurde dem Benutzer <#00cc88>[USER]</#00cc88> entfernt.")
    ),
    PERMISSION_GROUP_PARENT_REMOVE_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Parent <#009999>[PARENT]</#009999> is now removed from group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Der Elternteil <#009999>[PARENT]</#009999> wurde der Gruppe <#00cc88>[GROUP]</#00cc88> entfernt.")
    ),
    PERMISSION_USER_PERMISSION_CLEAR_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to clear permissions for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Löschen der Berechtigungen für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PERMISSION_CLEAR_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to clear permissions for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Löschen der Berechtigungen für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PARENT_CLEAR_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to clear parents for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Löschen der Elternteile für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PARENT_CLEAR_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to clear parents for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Löschen der Elternteile für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PERMISSION_CLEAR_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>All permissions for user <#00cc88>[USER]</#00cc88> are now cleared."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Alle Berechtigungen für den Benutzer <#00cc88>[USER]</#00cc88> wurden gelöscht.")
    ),
    PERMISSION_GROUP_PERMISSION_CLEAR_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>All permissions for group <#00cc88>[GROUP]</#00cc88> are now cleared."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Alle Berechtigungen für die Gruppe <#00cc88>[GROUP]</#00cc88> wurden gelöscht.")
    ),
    PERMISSION_USER_PARENT_CLEAR_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>All parents for user <#00cc88>[USER]</#00cc88> are now cleared."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Alle Elternteile für den Benutzer <#00cc88>[USER]</#00cc88> wurden gelöscht.")
    ),
    PERMISSION_GROUP_PARENT_CLEAR_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>All parents for group <#00cc88>[GROUP]</#00cc88> are now cleared."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Alle Elternteile für die Gruppe <#00cc88>[GROUP]</#00cc88> wurden gelöscht.")
    ),
    PERMISSION_USER_PERMISSION_TIME_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to set time for permission <#999900>[PERMISSION]</#999900> for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Setzen der Zeit für die Berechtigung <#999900>[PERMISSION]</#999900> für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PERMISSION_TIME_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to set time for permission <#999900>[PERMISSION]</#999900> for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Setzen der Zeit für die Berechtigung <#999900>[PERMISSION]</#999900> für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PARENT_TIME_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to set time for parent <#999900>[PARENT]</#999900> for user <#00cc88>[USER]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Setzen der Zeit für den Elternteil <#999900>[PARENT]</#999900> für den Benutzer <#00cc88>[USER]</#00cc88>.")
    ),
    PERMISSION_GROUP_PARENT_TIME_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to set time for parent <#999900>[PARENT]</#999900> for group <#00cc88>[GROUP]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Setzen der Zeit für den Elternteil <#999900>[PARENT]</#999900> für die Gruppe <#00cc88>[GROUP]</#00cc88>.")
    ),
    PERMISSION_USER_PERMISSION_TIME_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Time for permission <#009999>[PERMISSION]</#009999> for user <#00cc88>[USER]</#00cc88> is now set to <#009999>[EXPIRED]</#009999>."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Die Zeit für die Berechtigung <#009999>[PERMISSION]</#009999> für den Benutzer <#00cc88>[USER]</#00cc88> wurde auf <#009999>[EXPIRED]</#009999> gesetzt.")
    ),
    PERMISSION_GROUP_PERMISSION_TIME_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Time for permission <#009999>[PERMISSION]</#009999> for group <#00cc88>[GROUP]</#00cc88> is now set to <#009999>[EXPIRED]</#009999>."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Die Zeit für die Berechtigung <#009999>[PERMISSION]</#009999> für die Gruppe <#00cc88>[GROUP]</#00cc88> wurde auf <#009999>[EXPIRED]</#009999> gesetzt.")
    ),
    PERMISSION_USER_PARENT_TIME_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Time for parent <#009999>[PARENT]</#009999> for user <#00cc88>[USER]</#00cc88> is now set to <#009999>[EXPIRED]</#009999>."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Die Zeit für den Elternteil <#009999>[PARENT]</#009999> für den Benutzer <#00cc88>[USER]</#00cc88> wurde auf <#009999>[EXPIRED]</#009999> gesetzt.")
    ),
    PERMISSION_GROUP_PARENT_TIME_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Time for parent <#009999>[PARENT]</#009999> for group <#00cc88>[GROUP]</#00cc88> is now set to <#009999>[EXPIRED]</#009999>."),
            of(getLanguageId(Lang.GERMAN), "<#999999>Die Zeit für den Elternteil <#009999>[PARENT]</#009999> für die Gruppe <#00cc88>[GROUP]</#00cc88> wurde auf <#009999>[EXPIRED]</#009999> gesetzt.")
    ),
    PERMISSION_INFO_CHANGE_STUFF(
            of(getLanguageId(Lang.ENGLISH), "<#999999>Changed by <#00cc88>[CHANGED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CHANGED_ID]</#00cc88>)</#555555> at <#00cc88>[CHANGED_AT]</#00cc88>"),
            of(getLanguageId(Lang.GERMAN), "<#999999>Geändert von <#00cc88>[CHANGED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CHANGED_ID]</#00cc88>)</#555555> am <#00cc88>[CHANGED_AT]</#00cc88>")
    ),
    PERMISSION_USER_PERMISSION_INFO_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), """
                    <#999999>===- Permission info
                    <#999999>User: <#00cc88>[USER_NAME]</#00cc88> <#555555>(ID: <#00cc88>[USER_ID]</#00cc88>)</#555555>
                    <#999999>Permission: <#00cc88>[PERMISSION]</#00cc88>
                    <#999999>Expires at: <#00cc88>[EXPIRES]</#00cc88>
                    <#999999>Created by: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> at <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]"""),
            of(getLanguageId(Lang.GERMAN), """
                    <#999999>===- Berechtigungsinfo
                    <#999999>Benutzer: <#00cc88>[USER_NAME]</#00cc88> <#555555>(ID: <#00cc88>[USER_ID]</#00cc88>)</#555555>
                    <#999999>Berechtigung: <#00cc88>[PERMISSION]</#00cc88>
                    <#999999>Läuft ab: <#00cc88>[EXPIRES]</#00cc88>
                    <#999999>Erstellt von: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> am <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]""")
    ),
    PERMISSION_GROUP_PERMISSION_INFO_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), """
                    <#999999>===- Permission info
                    <#999999>Group: <#00cc88>[GROUP_NAME]</#00cc88> <#555555>(ID: <#00cc88>[GROUP_ID]</#00cc88>)</#555555>
                    <#999999>Permission: <#00cc88>[PERMISSION]</#00cc88>
                    <#999999>Expires at: <#00cc88>[EXPIRES]</#00cc88>
                    <#999999>Created by: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> at <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]"""),
            of(getLanguageId(Lang.GERMAN), """
                    <#999999>===- Berechtigungsinfo
                    <#999999>Gruppe: <#00cc88>[GROUP_NAME]</#00cc88> <#555555>(ID: <#00cc88>[GROUP_ID]</#00cc88>)</#555555>
                    <#999999>Berechtigung: <#00cc88>[PERMISSION]</#00cc88>
                    <#999999>Läuft ab: <#00cc88>[EXPIRES]</#00cc88>
                    <#999999>Erstellt von: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> am <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]""")
    ),
    PERMISSION_USER_PARENT_INFO_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), """
                    <#999999>===- Parent info
                    <#999999>User: <#00cc88>[USER_NAME]</#00cc88> <#555555>(ID: <#00cc88>[USER_ID]</#00cc88>)</#555555>
                    <#999999>Parent: <#00cc88>[PARENT_NAME]</#00cc88> <#555555>(ID: <#00cc88>[PARENT_ID]</#00cc88>)</#555555>
                    <#999999>Expires at: <#00cc88>[EXPIRES]</#00cc88>
                    <#999999>Created by: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> at <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]"""),
            of(getLanguageId(Lang.GERMAN), """
                    <#999999>===- Elternteil Info
                    <#999999>Benutzer: <#00cc88>[USER_NAME]</#00cc88> <#555555>(ID: <#00cc88>[USER_ID]</#00cc88>)</#555555>
                    <#999999>Elternteil: <#00cc88>[PARENT_NAME]</#00cc88> <#555555>(ID: <#00cc88>[PARENT_ID]</#00cc88>)</#555555>
                    <#999999>Läuft ab: <#00cc88>[EXPIRES]</#00cc88>
                    <#999999>Erstellt von: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> am <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]""")
    ),
    PERMISSION_GROUP_PARENT_INFO_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), """
                    <#999999>===- Parent info
                    <#999999>Group: <#00cc88>[GROUP_NAME]</#00cc88> <#555555>(ID: <#00cc88>[GROUP_ID]</#00cc88>)</#555555>
                    <#999999>Parent: <#00cc88>[PARENT_NAME]</#00cc88> <#555555>(ID: <#00cc88>[PARENT_ID]</#00cc88>)</#555555>
                    <#999999>Expires at: <#00cc88>[EXPIRES]</#00cc88>
                    <#999999>Created by: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> at <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]"""),
            of(getLanguageId(Lang.GERMAN), """
                    <#999999>===- Elternteil Info
                    <#999999>Gruppe: <#00cc88>[GROUP_NAME]</#00cc88> <#555555>(ID: <#00cc88>[GROUP_ID]</#00cc88>)</#555555>
                    <#999999>Elternteil: <#00cc88>[PARENT_NAME]</#00cc88> <#555555>(ID: <#00cc88>[PARENT_ID]</#00cc88>)</#555555>
                    <#999999>Läuft ab: <#00cc88>[EXPIRES]</#00cc88>
                    <#999999>Erstellt von: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> am <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]""")
    ),
    PERMISSION_FORMAT_EXPIRED_TIME(
            of(getLanguageId(Lang.ENGLISH), "<#555555>(Expires: <#00cc88><hover:show_text:'<#999999>Expires: <#00cc88>[EXPIRED_TIME]<br><#999999>Current time: </#999999>[CURRENT_TIME]'>[EXPIRED_AT]</hover></#00cc88>)"),
            of(getLanguageId(Lang.GERMAN), "<#555555>(Läuft ab: <#00cc88><hover:show_text:'<#999999>Läuft ab: <#00cc88>[EXPIRED_TIME]<br><#999999>Aktuelle Zeit: </#999999>[CURRENT_TIME]'>[EXPIRED_AT]</hover></#00cc88>)")
    ),
    PERMISSION_FORMAT_EXPIRED_INFO_TIME(
            of(getLanguageId(Lang.ENGLISH), "<hover:show_text:'<#999999>Expired: <#00cc88>[EXPIRED_TIME]<br><#999999>Current time: </#999999>[CURRENT_TIME]'>[EXPIRED_AT]</hover>"),
            of(getLanguageId(Lang.GERMAN), "<hover:show_text:'<#999999>Läuft ab: <#00cc88>[EXPIRED_TIME]<br><#999999>Aktuelle Zeit: </#999999>[CURRENT_TIME]'>[EXPIRED_AT]</hover>")
    ),
    PERMISSION_USER_LIST_HEADER(
            of(getLanguageId(Lang.ENGLISH), "<#999999>===- [HEADER_NAME] for user <#00cc88>[USER_NAME]</#00cc88> <#555555>(ID: <#00cc88>[USER_ID]</#00cc88>)</#555555>"),
            of(getLanguageId(Lang.GERMAN), "<#999999>===- [HEADER_NAME] für den Benutzer <#00cc88>[USER_NAME]</#00cc88> <#555555>(ID: <#00cc88>[USER_ID]</#00cc88>)</#555555>")
    ),
    PERMISSION_GROUP_LIST_HEADER(
            of(getLanguageId(Lang.ENGLISH), "<#999999>===- [HEADER_NAME] for group <#00cc88>[GROUP_NAME]</#00cc88> <#555555>(ID: <#00cc88>[GROUP_ID]</#00cc88>)</#555555>"),
            of(getLanguageId(Lang.GERMAN), "<#999999>===- [HEADER_NAME] für die Gruppe <#00cc88>[GROUP_NAME]</#00cc88> <#555555>(ID: <#00cc88>[GROUP_ID]</#00cc88>)</#555555>")
    ),
    PERMISSIONS_LIST_SINGULAR(
            of(getLanguageId(Lang.ENGLISH), "Permission"),
            of(getLanguageId(Lang.GERMAN), "Berechtigung")
    ),
    PERMISSIONS_LIST_PLURAL(
            of(getLanguageId(Lang.ENGLISH), "Permissions"),
            of(getLanguageId(Lang.GERMAN), "Berechtigungen")
    ),
    PARENTS_LIST_SINGULAR(
            of(getLanguageId(Lang.ENGLISH), "Parent"),
            of(getLanguageId(Lang.GERMAN), "Elternteil")
    ),
    PARENTS_LIST_PLURAL(
            of(getLanguageId(Lang.ENGLISH), "Parents"),
            of(getLanguageId(Lang.GERMAN), "Elternteile")
    ),
    PERMISSION_LIST_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), "<#999999>- <#999900><hover:show_text:'<#990000>Click to remove <#999900>\"[PERMISSION]\"</#999900>'><click:suggest_command:[CLICK_COMMAND]>[PERMISSION]</click></hover>"),
            of(getLanguageId(Lang.GERMAN), "<#999999>- <#999900><hover:show_text:'<#990000>Klicke um <#999900>\"[PERMISSION]\"</#999900> zu entfernen'><click:suggest_command:[CLICK_COMMAND]>[PERMISSION]</click></hover>")
    ),
    PARENT_LIST_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), "<#999999>- <#999900><hover:show_text:'<#990000>Click to remove <#999900>\"[PARENT]\"</#999900>'><click:suggest_command:[CLICK_COMMAND]>[PARENT]</click></hover>"),
            of(getLanguageId(Lang.GERMAN), "<#999999>- <#999900><hover:show_text:'<#990000>Klicke um <#999900>\"[PARENT]\"</#999900> zu entfernen'><click:suggest_command:[CLICK_COMMAND]>[PARENT]</click></hover>")
    ),
    PARENT_LIST_DEFAULT_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), "<#999999>- <#999900>[DEFAULT_PARENT]"),
            of(getLanguageId(Lang.GERMAN), "<#999999>- <#999900>[DEFAULT_PARENT]")
    ),
    PERMISSION_LIST_FROM_GROUP_PART(
            of(getLanguageId(Lang.ENGLISH), "<#999999>from <#00cc88>[GROUP_NAME]</#00cc88>"),
            of(getLanguageId(Lang.GERMAN), "<#999999>von <#00cc88>[GROUP_NAME]</#00cc88>")
    ),
    PARSE_TIME_INVALID(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Invalid time format: <#999900>\"[TIME]\"</#999900>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Ungültiges Zeitformat: <#999900>\"[TIME]\"</#999900>.")
    ),
    PARE_TIME_NEGATIVE(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Time cannot be negative: <#999900>\"[TIME]\"</#999900>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Zeit kann nicht negativ sein: <#999900>\"[TIME]\"</#999900>.")
    ),
    DEFAULT_GROUP_NOT_FOUND(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Default group not found."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Standardgruppe nicht gefunden.")
    ),
    DEFAULT_GROUP_ADD(
            of(getLanguageId(Lang.ENGLISH), "<#990000>You cannot add default group as a parent."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Du kannst die Standardgruppe nicht als Elternteil hinzufügen.")
    ),
    DEFAULT_GROUP_REMOVE(
            of(getLanguageId(Lang.ENGLISH), "<#990000>You cannot remove default parent."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Du kannst den Standard-Elternteil nicht entfernen.")
    ),
    DEFAULT_GROUP_TIME(
            of(getLanguageId(Lang.ENGLISH), "<#990000>You cannot set time for default group."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Du kannst keine Zeit für die Standardgruppe setzen.")
    ),
    GROUP_NOT_FOUND(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Group <#999900>[GROUP]</#999900> not found."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Gruppe <#999900>[GROUP]</#999900> nicht gefunden.")
    ),
    USER_NOT_FOUND(
            of(getLanguageId(Lang.ENGLISH), "<#990000>User <#999900>[USER]</#999900> not found."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Benutzer <#999900>[USER]</#999900> nicht gefunden.")
    ),
    DEBUG_PREFIX(
            of(getLanguageId(Lang.ENGLISH), "<#00CCdd>Debug <#454545>»</#454545> <#888800>"),
            of(getLanguageId(Lang.GERMAN), "<#00CCdd>Debug <#454545>»</#454545> <#888800>")
    ),
    PERMISSION_GROUP_COLOR_VALUE(
            of(getLanguageId(Lang.ENGLISH), "<#999999><#009999>[COLOR_TYPE]</#009999> of <#cc8800>[GROUP_NAME]</#cc8800>: <#00cc88>[VALUE]</#00cc88>"),
            of(getLanguageId(Lang.GERMAN), "<#999999><#009999>[COLOR_TYPE]</#009999> von <#cc8800>[GROUP_NAME]</#cc8800>: <#00cc88>[VALUE]</#00cc88>")
    ),
    PERMISSION_GROUP_COLOR_NOT_SET(
            of(getLanguageId(Lang.ENGLISH), "<#990000><#009999>[COLOR_TYPE]</#009999> is not set for group <#cc8800>[GROUP_NAME]</#cc8800>."),
            of(getLanguageId(Lang.GERMAN), "<#990000><#009999>[COLOR_TYPE]</#009999> ist nicht für die Gruppe <#cc8800>[GROUP_NAME]</#cc8800> gesetzt.")
    ),
    PERMISSION_GROUP_COLOR_INFO_MESSAGE(
            of(getLanguageId(Lang.ENGLISH), """
                    <#999999>===- Color info
                    <#999999>Group: <#00cc88>[GROUP_NAME]</#00cc88> <#555555>(ID: <#00cc88>[GROUP_ID]</#00cc88>)</#555555>
                    <#999999>[COLOR_TYPE]: <#00cc88>[VALUE]</#00cc88>
                    <#999999>Created by: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> at <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]"""),
            of(getLanguageId(Lang.GERMAN), """
                    <#999999>===- Farbinfo
                    <#999999>Gruppe: <#00cc88>[GROUP_NAME]</#00cc88> <#555555>(ID: <#00cc88>[GROUP_ID]</#00cc88>)</#555555>
                    <#999999>[COLOR_TYPE]: <#00cc88>[VALUE]</#00cc88>
                    <#999999>Erstellt von: <#00cc88>[CREATED_NAME]</#00cc88> <#555555>(ID: <#00cc88>[CREATED_ID]</#00cc88>)</#555555> am <#00cc88>[CREATED_AT]</#00cc88>
                    [CHANGED]""")
    ),
    PERMISSION_GROUP_COLOR_ADD_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to add <#999900>[COLOR_TYPE]</#999900> for group <<#cc8800>[GROUP_NAME]</#cc8800>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Hinzufügen von <#999900>[COLOR_TYPE]</#999900> für die Gruppe <#cc8800>[GROUP_NAME]</#cc8800>.")
    ),
    PERMISSION_GROUP_COLOR_ADD_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999><#009999>[COLOR_TYPE]</#009999> of group <#cc8800>[GROUP_NAME]</#cc8800> is now set to <#00cc88>[VALUE]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#999999><#009999>[COLOR_TYPE]</#009999> der Gruppe <#cc8800>[GROUP_NAME]</#cc8800> ist jetzt auf <#00cc88>[VALUE]</#00cc88> gesetzt.")
    ),
    INVALID_COLOR_TYPE(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Invalid color type."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Ungültiger Farbtyp.")
    ),
    PERMISSION_GROUP_COLOR_UPDATE_FAILED(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Failed to update <#999900>[COLOR_TYPE]</#999900> for group <#cc8800>[GROUP_NAME]</#cc8800>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Fehler beim Aktualisieren von <#999900>[COLOR_TYPE]</#999900> für die Gruppe <#cc8800>[GROUP_NAME]</#cc8800>.")
    ),
    PERMISSION_GROUP_COLOR_UPDATE_SUCCESS(
            of(getLanguageId(Lang.ENGLISH), "<#999999><#009999>[COLOR_TYPE]</#009999> of group <#cc8800>[GROUP_NAME]</#cc8800> is now updated to <#00cc88>[VALUE]</#00cc88>."),
            of(getLanguageId(Lang.GERMAN), "<#999999><#009999>[COLOR_TYPE]</#009999> der Gruppe <#cc8800>[GROUP_NAME]</#cc8800> wurde auf <#00cc88>[VALUE]</#00cc88> aktualisiert.")
    ),
    PRIORITY_INVALID(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Invalid priority value: <#999900>[PRIORITY]</#999900>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Ungültiger Prioritätswert: <#999900>[PRIORITY]</#999900>.")
    ),
    PRIORITY_NEGATIVE(
            of(getLanguageId(Lang.ENGLISH), "<#990000>Priority cannot be negative: <#999900>[PRIORITY]</#999900>."),
            of(getLanguageId(Lang.GERMAN), "<#990000>Priorität kann nicht negativ sein: <#999900>[PRIORITY]</#999900>.")
    ),
    ;
    private static final Messages[] VALUES = values();
    private final Map<Integer, String> messages;

    Messages(LocalizedMessage... entries) {
        Map<Integer, String> map = new HashMap<>();
        for (LocalizedMessage entry : entries)
            map.put(entry.languageId(), entry.message());
        this.messages = Collections.unmodifiableMap(map);
    }

    @Override
    public String getTag() {
        return name().toUpperCase();
    }

    @Override
    public Map<Integer, String> getMessages() {
        return messages;
    }

    private static int getLanguageId(Lang lang) {
        return lang.getId();
    }

    public static void loadMessages(MessageService service) {
        service.checkAndLoad(VALUES);
    }

    private enum Lang {
        ENGLISH,
        GERMAN;
        private final LanguageProvider provider = MurmelAPI.getLanguageProvider();

        public int getId() {
            return provider.get(this.name().toLowerCase()).id();
        }
    }
}
