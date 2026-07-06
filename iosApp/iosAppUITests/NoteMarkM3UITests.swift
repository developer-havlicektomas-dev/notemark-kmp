import XCTest

/// End-to-end journey for Milestone 3 (Reading Experience & Settings):
/// login → create note → View mode (metadata) → Reader mode (auto-hide +
/// tap-to-show) → Edit mode (discard dialog, save) → delete → Settings → logout.
///
/// Credentials come from `TEST_RUNNER_*` environment variables so each run can
/// use a freshly registered user (see the defaults below for local runs).
final class NoteMarkM3UITests: XCTestCase {

    private var email: String { ProcessInfo.processInfo.environment["NM_EMAIL"] ?? "m3tester@example.com" }
    private var password: String { ProcessInfo.processInfo.environment["NM_PASSWORD"] ?? "Test12345!" }
    private var username: String { ProcessInfo.processInfo.environment["NM_USERNAME"] ?? "m3tester" }

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testMilestone3Journey() throws {
        let app = XCUIApplication()
        app.launch()

        logIn(app)
        createNote(app, title: "Groceries run", content: "Milk and eggs")

        // The saved note pops back to the list.
        let card = app.staticTexts["Groceries run"]
        XCTAssertTrue(card.waitForExistence(timeout: 10), "created note should appear on the list")
        snap(app, "notes_list")
        card.tap()

        // View mode: read-only body with the Created/Edited metadata line.
        let editButton = app.buttons["Edit"]
        XCTAssertTrue(editButton.waitForExistence(timeout: 10), "existing notes should open in View mode")
        let metadata = app.staticTexts.matching(NSPredicate(format: "label BEGINSWITH 'Created '")).firstMatch
        XCTAssertTrue(metadata.waitForExistence(timeout: 5), "View mode should show the Created/Edited metadata line")
        snap(app, "note_view_mode")

        // Reader mode: controls visible on entry…
        app.buttons["readerButton"].tap()
        let readerLabel = app.staticTexts["READER"]
        XCTAssertTrue(readerLabel.waitForExistence(timeout: 5), "reader controls should be visible on entry")
        snap(app, "reader_controls_visible")

        // …auto-hide after ~3 seconds…
        let hint = app.staticTexts["Tap anywhere to show controls"]
        XCTAssertTrue(hint.waitForExistence(timeout: 8), "controls should auto-hide and show the hint")
        XCTAssertFalse(readerLabel.exists, "the controls pill should be gone once hidden")
        snap(app, "reader_controls_hidden")

        // …and come back on tap.
        app.staticTexts["Groceries run"].tap()
        let readerBack = app.buttons["readerBackButton"]
        XCTAssertTrue(readerBack.waitForExistence(timeout: 5), "tapping should bring the controls back")
        readerBack.tap()

        // Back in View mode → switch to Edit mode.
        XCTAssertTrue(editButton.waitForExistence(timeout: 5))
        editButton.tap()
        XCTAssertTrue(app.staticTexts["Edit note"].waitForExistence(timeout: 5))
        snap(app, "note_edit_mode")

        let contentField = textEntry(app, id: "noteContentField")
        contentField.tap()
        contentField.typeText(" and bread")

        // Closing with unsaved changes must ask for confirmation; keep editing.
        app.buttons["editCloseButton"].tap()
        let discardSheet = app.sheets["Discard changes?"]
        XCTAssertTrue(discardSheet.waitForExistence(timeout: 5), "closing a dirty editor should ask to discard")
        discardSheet.buttons["Keep editing"].tap()

        app.buttons["Save"].tap()

        // Saving an existing note stays on the note, back in View mode.
        XCTAssertTrue(editButton.waitForExistence(timeout: 10), "saving should return to View mode")
        let updated = app.staticTexts.matching(NSPredicate(format: "label CONTAINS 'and bread'")).firstMatch
        XCTAssertTrue(updated.waitForExistence(timeout: 5), "saved content should be visible in View mode")

        // Back to the list, delete the note via long-press.
        app.buttons["detailBackButton"].tap()
        XCTAssertTrue(card.waitForExistence(timeout: 10))
        card.press(forDuration: 1.0)
        let deleteSheet = app.sheets["Delete this note?"]
        XCTAssertTrue(deleteSheet.waitForExistence(timeout: 5), "long-press should offer deletion")
        deleteSheet.buttons["Delete"].tap()
        XCTAssertTrue(app.buttons["New note"].waitForExistence(timeout: 10), "list should be empty after deleting the only note")

        // Settings: profile card + logout back to the auth flow.
        app.buttons["avatarButton"].tap()
        XCTAssertTrue(app.staticTexts["Settings"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts[username].exists, "settings should show the signed-in username")
        snap(app, "settings")
        // M4 replaced the simple confirm with a 3-option dialog.
        app.buttons["logoutButton"].tap()
        let logoutSheet = app.sheets["Log out?"]
        XCTAssertTrue(logoutSheet.waitForExistence(timeout: 5))
        logoutSheet.buttons["Sync and log out"].tap()

        XCTAssertTrue(app.buttons["Log in"].waitForExistence(timeout: 10), "logout should land back on the auth flow")
    }

    // MARK: - Helpers

    /// Attaches a named screenshot to the test report for visual review.
    private func snap(_ app: XCUIApplication, _ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func logIn(_ app: XCUIApplication) {
        let landingLogIn = app.buttons["Log in"]
        XCTAssertTrue(landingLogIn.waitForExistence(timeout: 10), "fresh install should show the landing screen")
        landingLogIn.tap()

        let emailField = app.textFields["you@example.com"]
        XCTAssertTrue(emailField.waitForExistence(timeout: 5))
        emailField.tap()
        emailField.typeText(email)

        let passwordField = app.secureTextFields["Password"]
        passwordField.tap()
        // Submit via the keyboard's "go" to avoid the two same-named "Log in" buttons.
        passwordField.typeText(password + "\n")

        XCTAssertTrue(app.staticTexts["NoteMark"].waitForExistence(timeout: 15), "login should land on the notes list")
    }

    private func createNote(_ app: XCUIApplication, title: String, content: String) {
        let emptyStateCreate = app.buttons["New note"]
        if emptyStateCreate.waitForExistence(timeout: 3) {
            emptyStateCreate.tap()
        } else {
            app.buttons["createNoteButton"].tap()
        }

        let titleField = textEntry(app, id: "noteTitleField")
        XCTAssertTrue(titleField.waitForExistence(timeout: 5), "a new note should open straight in Edit mode")
        titleField.tap()
        titleField.typeText(title)

        let contentField = textEntry(app, id: "noteContentField")
        contentField.tap()
        contentField.typeText(content)

        app.buttons["Save"].tap()
    }

    /// Vertical-axis TextFields can surface as text fields or text views in the AX tree.
    private func textEntry(_ app: XCUIApplication, id: String) -> XCUIElement {
        let field = app.textFields[id]
        return field.exists ? field : app.textViews[id]
    }
}
