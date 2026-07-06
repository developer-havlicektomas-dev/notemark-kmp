import SwiftUI

struct RegisterView: View {
    let onRegistered: (_ email: String) -> Void

    @StateObject private var viewModel = RegisterViewModel()
    @Environment(\.dismiss) private var dismiss

    private var repeatHelper: String? {
        guard !viewModel.repeatPassword.isEmpty, !viewModel.passwordsMatch else { return nil }
        return "Passwords don't match."
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                BackButton { dismiss() }
                Spacer()
            }
            .frame(height: 44)
            .padding(.horizontal, Space.sm)

            ScrollView {
                VStack(alignment: .leading, spacing: Space.lg) {
                    VStack(alignment: .leading, spacing: Space.xs) {
                        Text("Create your account")
                            .font(.nmTitle)
                            .foregroundStyle(AppColor.fg)
                        Text("Free, and synced across your devices.")
                            .font(.nmSubhead)
                            .foregroundStyle(AppColor.fgMuted)
                    }

                    AuthTextField(
                        label: "Username",
                        placeholder: "yourname",
                        text: $viewModel.username,
                        helper: "3 to 20 characters.",
                        textContentType: .username
                    )

                    AuthTextField(
                        label: "Email",
                        placeholder: "you@example.com",
                        text: $viewModel.email,
                        keyboard: .emailAddress,
                        textContentType: .emailAddress
                    )

                    AuthTextField(
                        label: "Password",
                        placeholder: "Password",
                        text: $viewModel.password,
                        helper: "At least 8 characters, with a number or symbol.",
                        isSecure: true,
                        textContentType: .newPassword
                    )

                    AuthTextField(
                        label: "Repeat password",
                        placeholder: "Repeat password",
                        text: $viewModel.repeatPassword,
                        helper: repeatHelper,
                        isSecure: true,
                        textContentType: .newPassword,
                        submitLabel: .go,
                        onSubmit: { viewModel.register(onSuccess: onRegistered) }
                    )

                    if let error = viewModel.errorMessage {
                        Text(error)
                            .font(.nmFootnote)
                            .foregroundStyle(AppColor.danger)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    PrimaryButton(
                        title: "Create account",
                        isLoading: viewModel.isRegistering,
                        isEnabled: viewModel.canRegister
                    ) {
                        viewModel.register(onSuccess: onRegistered)
                    }
                    .padding(.top, Space.xs)
                }
                .padding(.horizontal, 22)
                .padding(.top, Space.xs)
                .padding(.bottom, Space.huge)
                .frame(maxWidth: 380)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .background(AppColor.bg.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
        .toolbar(.hidden, for: .navigationBar)
    }
}
