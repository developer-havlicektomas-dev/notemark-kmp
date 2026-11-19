import XCTest

/// End-to-end journey for Milestone 4 (Offline-First) UI, online path:
/// login → new note autosaves while typing (status line, no Save tap) →
/// Settings sync section (Sync now + Last synced, Automatic sync picker) →
/// 3-option logout dialog → sync and log out.
///
/// Offline behaviors (banners, blocked logout, queue survival) can't be driven
/// from XCUITest — the simulator's network can't be toggled per-test — and are
/// covered by the shared-logic layer verified on Android.
final class NoteMarkM4UITests: XCTestCase {

    private var email: String { ProcessInfo.processInfo.environment["NM_EMAIL"] ?? "m4tester@example.com" }
    private var password: String { ProcessInfo.processInfo.environment["NM_PASSWORD"] ?? "Test12345!" }
    private var username: String { ProcessInfo.processInfo.environment["NM_USERNAME"] ?? "m4tester" }

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testMilestone4Journey() throws {
        let app = XCUIApplication()
        app.launch()

        logIn(app)

        // New note: type without ever tapping Save — autosave must persist it.
        openNewNote(app)
        let titleField = textEntry(app, id: "noteTitleField")
        titleField.tap()
        titleField.typeText("Autosaved draft")
        let contentField = textEntry(app, id: "noteContentField")
        contentField.tap()
        contentField.typeText("Typed and never saved manually")

        // The debounced autosave (800ms) surfaces the status line.
        let statusLine = app.descendants(matching: .any)["saveStatusLine"].firstMatch
        XCTAssertTrue(statusLine.waitForExistence(timeout: 8), "autosave should show the saved-status line")
        XCTAssertTrue(statusLine.label.contains("Saved"), "status line should read 'Saved …', got '\(statusLine.label)'")
        snap(app, "autosave_status_line")

        // Leaving via back offers to discard the autosaved draft; keep it via Save.
        app.buttons["Save"].tap()
        let card = app.staticTexts["Autosaved draft"]
        XCTAssertTrue(card.waitForExistence(timeout: 10), "autosaved note should be on the list")

        // Editing an existing note keeps autosaving under the same id.
        card.tap()
        let editButton = app.buttons["Edit"]
        XCTAssertTrue(editButton.waitForExistence(timeout: 10))
        editButton.tap()
        let editContent = textEntry(app, id: "noteContentField")
        editContent.tap()
        editContent.typeText(" — plus an autosaved edit")
        XCTAssertTrue(statusLine.waitForExistence(timeout: 8), "editing should re-trigger the autosave status line")

        // Discarding after an autosave must revert to the session snapshot.
        app.buttons["editCloseButton"].tap()
        let discardSheet = app.sheets["Discard changes?"]
        XCTAssertTrue(discardSheet.waitForExistence(timeout: 5), "back after autosaved edits should still ask")
        discardSheet.buttons["Discard"].tap()
        let reverted = app.staticTexts.matching(
            NSPredicate(format: "label == 'Typed and never saved manually'")
        ).firstMatch
        XCTAssertTrue(reverted.waitForExistence(timeout: 5), "discard should revert the autosaved edit")
        app.buttons["detailBackButton"].tap()

        // Settings: sync section.
        XCTAssertTrue(card.waitForExistence(timeout: 10))
        app.buttons["avatarButton"].tap()
        XCTAssertTrue(app.staticTexts["Settings"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts[username].exists)

        // Sync now: spinner runs, then the timestamp lands on "just now".
        let syncRow = app.buttons["syncNowRow"]
        XCTAssertTrue(syncRow.waitForExistence(timeout: 5), "settings should offer Sync now")
        syncRow.tap()
        let syncedJustNow = app.staticTexts.matching(
            NSPredicate(format: "label CONTAINS 'Last synced just now'")
        ).firstMatch
        XCTAssertTrue(syncedJustNow.waitForExistence(timeout: 15), "manual sync should update the Last synced line")
        snap(app, "settings_synced")

        // Automatic sync: pick 30 minutes, the row reflects it.
        app.buttons["autoSyncRow"].tap()
        let intervalSheet = app.sheets["Automatic sync"]
        XCTAssertTrue(intervalSheet.waitForExistence(timeout: 5), "interval picker should open")
        snap(app, "interval_picker")
        intervalSheet.buttons["30 minutes"].tap()
        let intervalValue = app.staticTexts["30 minutes"]
        XCTAssertTrue(intervalValue.waitForExistence(timeout: 5), "the row should show the chosen interval")

        // Logout: the 3-option dialog, cancel first, then sync and log out.
        app.buttons["logoutButton"].tap()
        let logoutSheet = app.sheets["Log out?"]
        XCTAssertTrue(logoutSheet.waitForExistence(timeout: 5), "logout should open the 3-option dialog")
        XCTAssertTrue(logoutSheet.buttons["Log out without syncing"].exists)
        snap(app, "logout_dialog")
        cancelDialog(app, logoutSheet, "Cancel")
        XCTAssertTrue(app.staticTexts["Settings"].waitForExistence(timeout: 5), "cancel should stay on Settings")

        app.buttons["logoutButton"].tap()
        XCTAssertTrue(logoutSheet.waitForExistence(timeout: 5))
        logoutSheet.buttons["Sync and log out"].tap()
        XCTAssertTrue(app.buttons["Log in"].waitForExistence(timeout: 15), "sync-and-logout should land on the auth flow")
    }

    // MARK: - Helpers

    /// Taps a dialog's cancel-role button. On recent iOS an iPhone dialog shown as a
    /// popover hides that button (tapping outside cancels), so fall back to that.
    private func cancelDialog(_ app: XCUIApplication, _ sheet: XCUIElement, _ label: String) {
        let button = sheet.buttons[label]
        if button.exists {
            button.tap()
        } else {
            app.otherElements["PopoverDismissRegion"].firstMatch.tap()
        }
    }

    private func snap(_ app: XCUIApplication, _ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }


    /// A previous run that failed midway can leave a session behind; start logged out.
    private func ensureLoggedOut(_ app: XCUIApplication) {
        let avatar = app.buttons["avatarButton"]
        guard avatar.waitForExistence(timeout: 3) else { return }
        avatar.tap()
        app.buttons["logoutButton"].tap()
        app.sheets["Log out?"].buttons["Sync and log out"].tap()
        _ = app.buttons["Log in"].waitForExistence(timeout: 15)
    }

    private func logIn(_ app: XCUIApplication) {
        ensureLoggedOut(app)
        let landingLogIn = app.buttons["Log in"]
        XCTAssertTrue(landingLogIn.waitForExistence(timeout: 10), "fresh install should show the landing screen")
        landingLogIn.tap()

        let emailField = app.textFields["you@example.com"]
        XCTAssertTrue(emailField.waitForExistence(timeout: 5))
        emailField.tap()
        emailField.typeText(email)

        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        passwordField.typeText(password + "\n")

        XCTAssertTrue(app.staticTexts["NoteMark"].waitForExistence(timeout: 15), "login should land on the notes list")

        // On real devices iOS may offer to save the password; its sheet blocks taps.
        let savePassword = app.sheets["Save Password?"]
        if savePassword.waitForExistence(timeout: 3) {
            savePassword.buttons["Not Now"].tap()
        }
    }

    private func openNewNote(_ app: XCUIApplication) {
        let emptyStateCreate = app.buttons["New note"]
        if emptyStateCreate.waitForExistence(timeout: 3) {
            emptyStateCreate.tap()
        } else {
            app.buttons["createNoteButton"].tap()
        }
        XCTAssertTrue(textEntry(app, id: "noteTitleField").waitForExistence(timeout: 5),
                      "a new note should open straight in Edit mode")
    }

    /// Vertical-axis TextFields can surface as text fields or text views in the AX tree.
    private func textEntry(_ app: XCUIApplication, id: String) -> XCUIElement {
        let field = app.textFields[id]
        return field.exists ? field : app.textViews[id]
    }
}
