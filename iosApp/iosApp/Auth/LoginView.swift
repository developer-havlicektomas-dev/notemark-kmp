import SwiftUI

struct LoginView: View {
    let onLoggedIn: () -> Void
    let onRegister: () -> Void

    @StateObject private var viewModel = LoginViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                BackButton { dismiss() }
                Spacer()
            }
            .frame(height: 44)
            .padding(.horizontal, Space.sm)

            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    VStack(alignment: .leading, spacing: Space.xs) {
                        Text("Welcome back")
                            .font(.nmTitle)
                            .foregroundStyle(AppColor.fg)
                        Text("Log in to pick up where you left off.")
                            .font(.nmSubhead)
                            .foregroundStyle(AppColor.fgMuted)
                    }

                    AuthTextField(
                        label: "Email",
                        placeholder: "you@example.com",
                        text: $viewModel.email,
                        keyboard: .emailAddress,
                        textContentType: .username
                    )

                    AuthTextField(
                        label: "Password",
                        placeholder: "Password",
                        text: $viewModel.password,
                        isSecure: true,
                        textContentType: .password,
                        submitLabel: .go,
                        onSubmit: { viewModel.login(onSuccess: onLoggedIn) }
                    )

                    if let error = viewModel.errorMessage {
                        Text(error)
                            .font(.nmFootnote)
                            .foregroundStyle(AppColor.danger)
                            .fixedSize(horizontal: false, vertical: true)
                    }

                    PrimaryButton(
                        title: "Log in",
                        isLoading: viewModel.isLoggingIn,
                        isEnabled: viewModel.canLogin
                    ) {
                        viewModel.login(onSuccess: onLoggedIn)
                    }

                    HStack(spacing: 4) {
                        Text("No account yet?")
                            .foregroundStyle(AppColor.fgSubtle)
                        Button(action: onRegister) {
                            Text("Sign up")
                                .foregroundStyle(AppColor.accent)
                                .fontWeight(.semibold)
                        }
                        .buttonStyle(.plain)
                    }
                    .font(.system(size: 14))
                    .frame(maxWidth: .infinity)
                    .padding(.top, Space.xs)
                }
                .padding(.horizontal, 22)
                .padding(.top, Space.sm)
                .padding(.bottom, Space.xxl)
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
