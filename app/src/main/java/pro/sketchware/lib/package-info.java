/**
 * Internal infrastructure for editor subsystems.
 *
 * <p>This package holds shared building blocks used <em>inside</em> specific editor features
 * (code editor, syntax highlighter, icon creator), <em>not</em> general-purpose UI controls.
 *
 * <h2>Boundary vs other packages</h2>
 * <ul>
 *   <li>{@code pro.sketchware.lib} (this package) — editor-feature-internal scaffolding.
 *       Examples: {@code code_editor/CodeEditorEditText}, {@code highlighter/SyntaxScheme},
 *       {@code iconcreator/PatternBackgroundView}, {@code base/BaseViewBindingAdapter}.
 *       Classes here are typically referenced only by their owning feature.</li>
 *   <li>{@link pro.sketchware.widgets} — general-purpose widgets that can be referenced
 *       directly by FQN tag in {@code res/layout/*.xml} (e.g. {@code CircleImageView},
 *       {@code EasyDeleteEditText}). Concrete classes intended to be instantiated.</li>
 *   <li>{@link pro.sketchware.widgets.base} — abstract base classes for widgets,
 *       intended to be extended (not instantiated directly).</li>
 * </ul>
 *
 * <h2>When to add a class here</h2>
 * If the class only makes sense inside a single editor subsystem and is not consumed via
 * XML layout tags, this package is the right home. Otherwise prefer {@code widgets/} or
 * {@code widgets/base/}.
 */
package pro.sketchware.lib;
