/**
 * General-purpose UI widgets — concrete implementations.
 *
 * <p>This package holds widget classes that are consumed by other features as concrete
 * components, typically via FQN tags in {@code res/layout/*.xml} (e.g.
 * {@code <pro.sketchware.widgets.CircleImageView .../>}) or by direct instantiation.
 *
 * <h2>Boundary vs other packages</h2>
 * <ul>
 *   <li>{@code pro.sketchware.widgets} (this package) — concrete, instantiable widgets.
 *       Examples: {@code CircleImageView}, {@code EasyDeleteEditText},
 *       {@code CustomScrollView}, {@code CustomViewPager}, {@code LoadingDialog},
 *       {@code ColorPickerDialog}.</li>
 *   <li>{@link pro.sketchware.widgets.base} — abstract base classes for widgets,
 *       intended to be extended (e.g. {@code BaseWidget}, {@code CollapsibleLayout}).
 *       Do not instantiate these directly.</li>
 *   <li>{@link pro.sketchware.lib} — editor-feature-internal scaffolding (code_editor,
 *       highlighter, iconcreator). Use that package for classes only meaningful inside
 *       a single editor subsystem.</li>
 * </ul>
 *
 * <h2>When to add a class here</h2>
 * If the class is a reusable UI control intended to appear in layouts or be reused
 * across features, this is the right home. If it only makes sense inside one editor
 * subsystem, use {@link pro.sketchware.lib} instead. If it is an abstract base for
 * other widgets, use {@link pro.sketchware.widgets.base}.
 */
package pro.sketchware.widgets;
