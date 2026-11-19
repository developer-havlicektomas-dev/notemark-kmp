// Renders the NoteMark app icon PNGs for the iOS asset catalog.
//
//   swift iosApp/scripts/render_app_icon.swift iosApp/iosApp/Assets.xcassets/AppIcon.appiconset
//
// The mark is the wordmark in miniature: a bold geometric "N" followed by the
// green accent dot. Keep the geometry in sync with the Android vector layers in
// androidApp/src/main/res/drawable/ic_launcher_*.xml (same unit glyph, see below).

import CoreGraphics
import Foundation
import ImageIO
import UniformTypeIdentifiers

// MARK: - Glyph, in units of the letter height (1.0)

let stem = 0.24          // stroke width of the N
let letterWidth = 0.84
let gap = 0.07           // between the N and the dot
let dotRadius = 0.13
let markWidth = letterWidth + gap + 2 * dotRadius

struct Palette {
    let background: CGColor?   // nil = transparent
    let letter: CGColor
    let dot: CGColor
}

func rgb(_ hex: UInt32) -> CGColor {
    CGColor(
        srgbRed: CGFloat((hex >> 16) & 0xFF) / 255,
        green: CGFloat((hex >> 8) & 0xFF) / 255,
        blue: CGFloat(hex & 0xFF) / 255,
        alpha: 1
    )
}

let variants: [(file: String, palette: Palette)] = [
    // Default: near-black (AppColor.bg dark), white N, brand green dot.
    ("AppIcon.png", Palette(background: rgb(0x09090B), letter: rgb(0xFFFFFF), dot: rgb(0x12B76A))),
    // Dark appearance: transparent so iOS supplies its dark backdrop; brighter dark-mode accent.
    ("AppIcon-Dark.png", Palette(background: nil, letter: rgb(0xFAFAFA), dot: rgb(0x32D583))),
    // Tinted appearance: grayscale, iOS tints by luminance.
    ("AppIcon-Tinted.png", Palette(background: rgb(0x000000), letter: rgb(0xFFFFFF), dot: rgb(0xBDBDBD))),
]

func render(size: Int, palette: Palette) -> CGImage {
    let alpha: CGImageAlphaInfo = palette.background == nil ? .premultipliedLast : .noneSkipLast
    let ctx = CGContext(
        data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
        space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: alpha.rawValue
    )!
    let s = CGFloat(size)
    if let bg = palette.background {
        ctx.setFillColor(bg)
        ctx.fill(CGRect(x: 0, y: 0, width: s, height: s))
    }

    // Letter height ~42% of the canvas; mark centred.
    let h = s * 0.42
    let originX = (s - CGFloat(markWidth) * h) / 2
    let top = (s - h) / 2
    // CoreGraphics' origin is bottom-left; work in top-left units and flip here.
    func p(_ x: Double, _ y: Double) -> CGPoint {
        CGPoint(x: originX + CGFloat(x) * h, y: s - (top + CGFloat(y) * h))
    }

    ctx.setFillColor(palette.letter)
    for poly in [
        [p(0, 0), p(stem, 0), p(stem, 1), p(0, 1)],                                       // left stem
        [p(letterWidth - stem, 0), p(letterWidth, 0), p(letterWidth, 1), p(letterWidth - stem, 1)], // right stem
        [p(0, 0), p(stem, 0), p(letterWidth, 1), p(letterWidth - stem, 1)],               // diagonal
    ] {
        ctx.addLines(between: poly)
        ctx.closePath()
        ctx.fillPath()
    }

    ctx.setFillColor(palette.dot)
    let c = p(letterWidth + gap + dotRadius, 1 - dotRadius)
    let r = CGFloat(dotRadius) * h
    ctx.fillEllipse(in: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r))
    return ctx.makeImage()!
}

let outDir = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : ".")
for variant in variants {
    let url = outDir.appendingPathComponent(variant.file)
    let dest = CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString, 1, nil)!
    CGImageDestinationAddImage(dest, render(size: 1024, palette: variant.palette), nil)
    guard CGImageDestinationFinalize(dest) else { fatalError("Couldn't write \(url.path)") }
    print("wrote \(url.path)")
}
