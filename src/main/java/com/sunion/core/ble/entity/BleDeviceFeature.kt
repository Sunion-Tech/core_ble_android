package com.sunion.core.ble.entity

class BleDeviceFeature{
    companion object {
        val initTaskList: Array<Triple<TaskCode, String, Set<String>>> = arrayOf(
            Triple(TaskCode.Connect, "Connect", setOf("1","2","3")),
            Triple(TaskCode.Disconnect, "Disconnect", setOf("1","2","3"))
        )

        val modelVersions: Map<String, Set<String>> = mapOf(
            "KD0" to setOf("1"),
            "TD0" to setOf("1"),
            "TLR0" to setOf("2"),
            "KDW00" to setOf("2"),
            "TNRFp00" to setOf("2"),
            "KD01" to setOf("3"),
        )

        val taskList: Array<Triple<TaskCode, String, Set<String>>> = arrayOf(
            Triple(TaskCode.Connect, "Connect", setOf("1","2","3")),
            Triple(TaskCode.GetDeviceStatus, "Get DeviceStatus", setOf("1","2","3")),
            Triple(TaskCode.ToggleLockState, "Toggle lock state", setOf("1","2","3")),
            Triple(TaskCode.IsAdminCodeExists, "Is Admin code exists", setOf("1","2","3")),
            Triple(TaskCode.CreateAdminCode, "Create Admin code", setOf("1","2","3")),
            Triple(TaskCode.UpdateAdminCode, "Update Admin code", setOf("1","2","3")),
            Triple(TaskCode.DetermineLockDirection, "Determine lock direction", setOf("1","2","3")),
            Triple(TaskCode.AutoUnlockToggleLockState, "Auto unlock toggle lock state", setOf("3")),
            Triple(TaskCode.GetLockName, "Get lock name", setOf("1","2","3")),
            Triple(TaskCode.SetLockName, "Set lock name", setOf("1","2","3")),
            Triple(TaskCode.GetLockTime, "Get lock time", setOf("1","2","3")),
            Triple(TaskCode.SetLockTime, "Set lock time", setOf("1","2","3")),
            Triple(TaskCode.GetLockTimeZone, "Get lock timeZone", setOf("1","2","3")),
            Triple(TaskCode.SetLockTimeZone, "Set lock timeZone", setOf("1","2","3")),
            Triple(TaskCode.GetLockConfig, "Get lock config", setOf("1","2","3")),
            Triple(TaskCode.SetLockLocation, "Set lock location", setOf("1","2","3")),
            Triple(TaskCode.ToggleSecurityBolt, "Toggle security bolt", setOf("2","3")),
            Triple(TaskCode.AutoUnlockToggleSecurityBolt, "Auto unlock toggle security bolt", setOf("3")),
            Triple(TaskCode.ToggleGuidingCode, "Toggle guiding code", setOf("1","2","3")),
            Triple(TaskCode.ToggleVirtualCode, "Toggle virtual code", setOf("2","3")),
            Triple(TaskCode.ToggleTwoFA, "Toggle twoFA", setOf("2","3")),
            Triple(TaskCode.ToggleVacationMode, "Toggle vacation mode", setOf("1","2","3")),
            Triple(TaskCode.ToggleAutoLock, "Toggle auto lock", setOf("1","2","3")),
            Triple(TaskCode.ToggleOperatingSound, "Toggle operating sound", setOf("2","3")),
            Triple(TaskCode.ToggleKeyPressBeep, "Toggle key press beep", setOf("1","2","3")),
            Triple(TaskCode.ToggleShowFastTrackMode, "Toggle show fast track mode", setOf("2","3")),
            Triple(TaskCode.ToggleSabbathMode, "Toggle Sabbath mode", setOf("3")),
            Triple(TaskCode.GetEventQuantity, "Get event quantity", setOf("1","2","3")),
            Triple(TaskCode.GetEvent, "Get event", setOf("1","2","3")),
            Triple(TaskCode.GetEventByAddress, "Get event by address", setOf("3")),
            Triple(TaskCode.DeleteEvent, "Delete event", setOf("1","2","3")),
            Triple(TaskCode.QueryTokenArray, "Query token array", setOf("1","2","3")),
            Triple(TaskCode.QueryToken, "Query token", setOf("1","2","3")),
            Triple(TaskCode.AddOneTimeToken, "Add one time token", setOf("1","2","3")),
            Triple(TaskCode.EditToken, "Edit token", setOf("1","2","3")),
            Triple(TaskCode.DeleteToken, "Delete token", setOf("1","2","3")),
            Triple(TaskCode.QueryUserAbility, "Query user ability", setOf("3")),
            Triple(TaskCode.QueryUserCount, "Query user count", setOf("3")),
            Triple(TaskCode.IsMatterDevice, "Is matter device", setOf("3")),
            Triple(TaskCode.GetUserArray, "Get user array", setOf("3")),
            Triple(TaskCode.GetUser, "Get user", setOf("3")),
            Triple(TaskCode.AddUser, "Add user", setOf("3")),
            Triple(TaskCode.EditUser, "Edit user", setOf("3")),
            Triple(TaskCode.DeleteUser, "Delete user", setOf("3")),
            Triple(TaskCode.GetCredentialArray, "Get credential array", setOf("3")),
            Triple(TaskCode.GetCredentialByCredential, "Get credential by credential", setOf("3")),
            Triple(TaskCode.GetCredentialByUser, "Get credential by user", setOf("3")),
            Triple(TaskCode.GetLockSupportedUnlockTypes, "Get lock supported unlock types", setOf("2")),
            Triple(TaskCode.GetAccessCodeArray, "Get access code array", setOf("1","2")),
            Triple(TaskCode.QueryAccessCode, "Query access code", setOf("1","2")),
            Triple(TaskCode.AddAccessCode, "Add access code", setOf("1","2","3")),
            Triple(TaskCode.EditAccessCode, "Edit access code", setOf("1","2","3")),
            Triple(TaskCode.DeleteAccessCode, "Delete access code", setOf("1","2","3")),
            Triple(TaskCode.GetAccessCardArray, "Get access card array", setOf("2")),
            Triple(TaskCode.QueryAccessCard, "Query access card", setOf("2")),
            Triple(TaskCode.AddAccessCard, "Add access card", setOf("2","3")),
            Triple(TaskCode.EditAccessCard, "Edit access card", setOf("2","3")),
            Triple(TaskCode.DeleteAccessCard, "Delete access card", setOf("2","3")),
            Triple(TaskCode.DeviceGetAccessCard, "Device get access card", setOf("2","3")),
            Triple(TaskCode.GetFingerprintArray, "Get fingerprint array", setOf("2")),
            Triple(TaskCode.QueryFingerprint, "Query fingerprint", setOf("2")),
            Triple(TaskCode.AddFingerprint, "Add fingerprint", setOf("2","3")),
            Triple(TaskCode.EditFingerprint, "Edit fingerprint", setOf("2","3")),
            Triple(TaskCode.DeleteFingerprint, "Delete fingerprint", setOf("2","3")),
            Triple(TaskCode.DeviceGetFingerprint, "Device get fingerprint", setOf("2","3")),
            Triple(TaskCode.GetFaceArray, "Get face array", setOf("2")),
            Triple(TaskCode.QueryFace, "Query face", setOf("2")),
            Triple(TaskCode.AddFace, "Add face", setOf("2","3")),
            Triple(TaskCode.EditFace, "Edit face", setOf("2","3")),
            Triple(TaskCode.DeleteFace, "Delete face", setOf("2","3")),
            Triple(TaskCode.DeviceGetFace, "Device get face", setOf("2","3")),
            Triple(TaskCode.AddCredentialFingerVein, "Add credential finger vein", setOf()),
            Triple(TaskCode.EditCredentialFingerVein, "Edit credential finger vein", setOf()),
            Triple(TaskCode.DeleteCredentialFingerVein, "Delete credential finger vein", setOf()),
            Triple(TaskCode.DeviceGetCredentialFingerVein, "Device get credential finger vein", setOf()),
            Triple(TaskCode.GetCredentialHash, "Get credential hash", setOf("3")),
            Triple(TaskCode.GetUserHash, "Get user hash", setOf("3")),
            Triple(TaskCode.HasUnsyncedData, "Has unsynced data", setOf("3")),
            Triple(TaskCode.GetUnsyncedData, "Get unsynced data", setOf("3")),
            Triple(TaskCode.SetCredentialUnsyncedData, "Set credential unsynced data", setOf("3")),
            Triple(TaskCode.SetUserUnsyncedData, "Set user unsynced data", setOf("3")),
            Triple(TaskCode.SetLogUnsyncedData, "Set log unsynced data", setOf("3")),
            Triple(TaskCode.SetTokenUnsyncedData, "Set token unsynced data", setOf("3")),
            Triple(TaskCode.SetSettingUnsyncedData, "Set setting unsynced data", setOf("3")),
            Triple(TaskCode.SetAllDataSynced, "Set all data synced", setOf("3")),
            Triple(TaskCode.SetOTAUpdate, "OTA update", setOf("2","3")),
            Triple(TaskCode.SetOTACancel, "OTA cancel", setOf("2","3")),
            Triple(TaskCode.ScanWifi, "Scan wifi", setOf("2","3")),
            Triple(TaskCode.ConnectToWifi, "Connect to wifi", setOf("2","3")),
            Triple(TaskCode.TogglePlugState, "Toggle plug state", setOf("3")),
            Triple(TaskCode.GetFwVersion, "Get firmware version", setOf("1","2","3")),
            Triple(TaskCode.GetRfVersion, "Get RF version", setOf("3")),
            Triple(TaskCode.GetMcuVersion, "Get MCU version", setOf("3")),
            Triple(TaskCode.Restart, "Restart", setOf("3")),
            Triple(TaskCode.FactoryReset, "Factory reset", setOf("1","2","3")),
            Triple(TaskCode.FactoryResetNoAdmin, "Factory reset no admin", setOf("2","3")),
            Triple(TaskCode.Disconnect, "Disconnect", setOf("1","2","3"))
        )
    }

    enum class TaskCode {
        Connect,
        GetLockTime,
        SetLockTime,
        SetLockTimeZone,
        GetDeviceStatus,
        IsAdminCodeExists,
        CreateAdminCode,
        UpdateAdminCode,
        GetLockName,
        SetLockName,
        ToggleLockState,
        DetermineLockDirection,
        ToggleKeyPressBeep,
        ToggleVacationMode,
        GetLockConfig,
        ToggleGuidingCode,
        ToggleAutoLock,
        SetLockLocation,
        QueryTokenArray,
        QueryToken,
        AddOneTimeToken,
        EditToken,
        DeleteToken,
        GetAccessCodeArray,
        QueryAccessCode,
        AddAccessCode,
        EditAccessCode,
        DeleteAccessCode,
        GetAccessCardArray,
        QueryAccessCard,
        AddAccessCard,
        EditAccessCard,
        DeleteAccessCard,
        DeviceGetAccessCard,
        GetFingerprintArray,
        QueryFingerprint,
        AddFingerprint,
        EditFingerprint,
        DeleteFingerprint,
        DeviceGetFingerprint,
        GetFaceArray,
        QueryFace,
        AddFace,
        EditFace,
        DeleteFace,
        DeviceGetFace,
        AddCredentialFingerVein,
        EditCredentialFingerVein,
        DeleteCredentialFingerVein,
        DeviceGetCredentialFingerVein,
        GetEventQuantity,
        GetEvent,
        GetEventByAddress,
        DeleteEvent,
        ToggleSecurityBolt,
        ToggleVirtualCode,
        ToggleTwoFA,
        ToggleOperatingSound,
        ToggleShowFastTrackMode,
        GetLockSupportedUnlockTypes,
        ScanWifi,
        ConnectToWifi,
        SetOTAUpdate,
        SetOTACancel,
        TogglePlugState,
        GetLockTimeZone,
        ToggleSabbathMode,
        AutoUnlockToggleLockState,
        AutoUnlockToggleSecurityBolt,
        QueryUserAbility,
        QueryUserCount,
        IsMatterDevice,
        GetUserArray,
        GetUser,
        AddUser,
        EditUser,
        DeleteUser,
        GetCredentialArray,
        GetCredentialByCredential,
        GetCredentialByUser,
        GetCredentialHash,
        GetUserHash,
        HasUnsyncedData,
        GetUnsyncedData,
        SetCredentialUnsyncedData,
        SetUserUnsyncedData,
        SetLogUnsyncedData,
        SetTokenUnsyncedData,
        SetSettingUnsyncedData,
        SetAllDataSynced,
        GetFwVersion,
        GetRfVersion,
        GetMcuVersion,
        FactoryReset,
        FactoryResetNoAdmin,
        Restart,
        Disconnect,
        Unknown
    }
}
