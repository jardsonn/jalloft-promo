package com.jalloft.promo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.jalloft.promo.IconLoader
import com.jalloft.promo.JalloftPromo
import com.jalloft.promo.PlayStore
import com.jalloft.promo.PromoApp
import com.jalloft.promo.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Tela "Mais apps para você": os outros apps da Jalloft, com os lançamentos
 * (NOVO) num carrossel no topo e o resto numa lista agrupada. O app atual sai
 * da lista automaticamente.
 *
 * @param onBack chamado pelo "‹ Voltar" (o voltar do sistema fica com a navegação do app).
 * @param monoFontFamily fonte dos rótulos em caixa-alta; o design usa JetBrains Mono.
 * @param onAppClick para logar no Analytics do app: package tocado e se já estava instalado.
 */
@Composable
fun MoreAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    monoFontFamily: FontFamily = FontFamily.Monospace,
    showFeatured: Boolean = true,
    onAppClick: (packageName: String, installed: Boolean) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var apps by remember { mutableStateOf<List<PromoApp>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var installed by remember { mutableStateOf(emptySet<String>()) }
    var pending by remember { mutableStateOf(emptySet<String>()) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reload) {
        failed = false
        if (apps == null) apps = JalloftPromo.cachedApps(context)
        try {
            apps = JalloftPromo.fetchApps(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Com cache na tela, falha de rede passa em silêncio
            if (apps == null) failed = true
        }
    }

    fun refreshInstalled() {
        installed = apps.orEmpty()
            .filter { PlayStore.isInstalled(context, it.packageName) }
            .mapTo(mutableSetOf()) { it.packageName }
    }
    LaunchedEffect(apps) { refreshInstalled() }
    // Voltando da Play Store: o app pode ter sido instalado → "Abrir"
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshInstalled()
        pending = emptySet()
    }

    val playToast = stringResource(R.string.jalloft_promo_toast_play)
    val openToast = stringResource(R.string.jalloft_promo_toast_open)

    MoreAppsContent(
        apps = apps,
        failed = failed,
        installed = installed,
        pending = pending,
        toast = toast,
        colors = if (darkTheme) PromoColors.Dark else PromoColors.Light,
        monoFontFamily = monoFontFamily,
        showFeatured = showFeatured,
        onBack = onBack,
        onRetry = { reload++ },
        onToastShown = { toast = null },
        onGet = { app ->
            onAppClick(app.packageName, false)
            pending = pending + app.packageName
            toast = playToast
            scope.launch {
                delay(350) // deixa o carregando aparecer antes de sair do app
                PlayStore.openListing(context, app.packageName)
                delay(2500)
                pending = pending - app.packageName
            }
        },
        onOpen = { app ->
            onAppClick(app.packageName, true)
            toast = openToast.format(app.shortName)
            if (!PlayStore.launch(context, app.packageName)) refreshInstalled()
        },
        modifier = modifier,
    )
}

private val PromoApp.shortName: String
    get() = name.substringBefore(':').substringBefore(" - ").trim()

private enum class ActionState { Get, Loading, Open }

@Composable
internal fun MoreAppsContent(
    apps: List<PromoApp>?,
    failed: Boolean,
    installed: Set<String>,
    pending: Set<String>,
    toast: String?,
    colors: PromoColors,
    monoFontFamily: FontFamily,
    showFeatured: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToastShown: () -> Unit,
    onGet: (PromoApp) -> Unit,
    onOpen: (PromoApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun stateOf(app: PromoApp) = when (app.packageName) {
        in installed -> ActionState.Open
        in pending -> ActionState.Loading
        else -> ActionState.Get
    }

    BoxWithConstraints(modifier.fillMaxSize().background(colors.sheet)) {
        val columns = when {
            maxWidth >= 900.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }
        val wide = columns > 1
        val pad = if (wide) 32.dp else 18.dp
        val featWidth = if (wide) 340.dp else 292.dp
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
        ) {
            // Só o "‹ Voltar" fica fixo, como a barra de navegação do iOS; o título grande rola junto
            BackButton(colors, onBack, Modifier.padding(horizontal = pad - 10.dp))

            val header: @Composable () -> Unit = { LargeTitle(pad, wide, colors, monoFontFamily) }

            when {
                apps == null && !failed -> SkeletonContent(columns, pad, featWidth, colors, header)

                apps == null -> ErrorState(colors, onRetry, header)

                else -> {
                    val featured = if (showFeatured) apps.filter { it.isNew } else emptyList()
                    val rows = apps.chunked(columns)

                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 40.dp + bottomInset),
                    ) {
                        item(key = "title") { header() }

                        if (featured.isNotEmpty()) {
                            item(key = "featured-header") {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = pad, end = pad, top = 18.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    SectionTitle(stringResource(R.string.jalloft_promo_featured), colors)
                                    BasicText(
                                        pluralStringResource(
                                            R.plurals.jalloft_promo_count, featured.size, featured.size
                                        ),
                                        style = TextStyle(color = colors.label2, fontSize = 15.sp),
                                    )
                                }
                            }
                            item(key = "featured") {
                                val state = rememberLazyListState()
                                LazyRow(
                                    state = state,
                                    flingBehavior = rememberSnapFlingBehavior(state),
                                    contentPadding = PaddingValues(start = pad, end = pad, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(featured, key = { it.packageName }) { app ->
                                        FeaturedCard(
                                            app = app,
                                            state = stateOf(app),
                                            colors = colors,
                                            monoFontFamily = monoFontFamily,
                                            onGet = { onGet(app) },
                                            onOpen = { onOpen(app) },
                                            modifier = Modifier.width(featWidth),
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "all-header") {
                            SectionTitle(
                                stringResource(R.string.jalloft_promo_all),
                                colors,
                                Modifier.padding(start = pad, end = pad, top = 26.dp, bottom = 10.dp),
                            )
                        }

                        // Um "cartão" agrupado, fatiado em linhas para continuar lazy
                        itemsIndexed(rows, key = { _, row -> row.first().packageName }) { i, row ->
                            val shape: Shape = when {
                                rows.size == 1 -> RoundedCornerShape(16.dp)
                                i == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                i == rows.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                else -> RectangleShape
                            }
                            Row(
                                Modifier
                                    .padding(horizontal = pad)
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .clip(shape)
                                    .background(colors.card)
                            ) {
                                row.forEach { app ->
                                    AppRow(
                                        app = app,
                                        state = stateOf(app),
                                        colors = colors,
                                        showSeparator = i != rows.lastIndex,
                                        onGet = { onGet(app) },
                                        onOpen = { onOpen(app) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }

                        item(key = "footer") {
                            BasicText(
                                stringResource(R.string.jalloft_promo_footer),
                                style = TextStyle(
                                    color = colors.label2,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = pad, end = pad, top = 16.dp),
                            )
                        }
                    }
                }
            }
        }

        Toast(
            text = toast,
            colors = colors,
            onShown = onToastShown,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp + bottomInset),
        )
    }
}

@Composable
private fun BackButton(colors: PromoColors, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .height(44.dp)
            .clickable(interaction, indication = null, onClick = onBack)
            .alpha(if (pressed) 0.4f else 1f)
            // O chevron fica a ~8dp da borda do frame de 24dp: compensa à esquerda
            // para ele alinhar com o título, como no design
            .padding(start = 2.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.jalloft_promo_ic_back),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(colors.accent),
        )
        BasicText(
            stringResource(R.string.jalloft_promo_back),
            style = TextStyle(color = colors.accent, fontSize = 17.sp),
        )
    }
}

/** "DO MESMO ESTÚDIO" + "Mais apps para você": o título grande, primeiro item do scroll. */
@Composable
private fun LargeTitle(pad: Dp, wide: Boolean, colors: PromoColors, monoFontFamily: FontFamily) {
    Column(
        Modifier.padding(start = pad, end = pad, top = 4.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BasicText(
            stringResource(R.string.jalloft_promo_eyebrow),
            style = mono(monoFontFamily, 11.sp, FontWeight.Bold, colors.accent),
        )
        BasicText(
            stringResource(R.string.jalloft_promo_title),
            style = TextStyle(
                color = colors.label,
                fontSize = if (wide) 34.sp else 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = tracking(-0.025f),
                lineHeight = 1.05.em,
            ),
        )
    }
}

@Composable
private fun SectionTitle(text: String, colors: PromoColors, modifier: Modifier = Modifier) {
    BasicText(
        text,
        modifier = modifier,
        style = TextStyle(
            color = colors.label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = tracking(-0.02f),
        ),
    )
}

@Composable
private fun FeaturedCard(
    app: PromoApp,
    state: ActionState,
    colors: PromoColors,
    monoFontFamily: FontFamily,
    onGet: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(colors.card)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .drawBehind { drawBanner(colors) },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(R.string.jalloft_promo_badge_new),
                style = mono(monoFontFamily, 10.sp, FontWeight.Bold, Color.White),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(colors.accent, RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 4.dp),
            )
            AppIcon(
                url = app.iconUrl,
                size = 92.dp,
                radius = 21.dp,
                colors = colors,
                modifier = Modifier.shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(21.dp),
                    ambientColor = Color.Black.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.18f),
                ),
            )
        }
        Row(
            Modifier.padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                app.category?.let {
                    BasicText(
                        it.uppercase(),
                        style = mono(monoFontFamily, 10.sp, FontWeight.Medium, colors.label2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AppName(app.name, colors)
            }
            ActionButton(state, colors, onGet, onOpen)
        }
    }
}

@Composable
private fun AppRow(
    app: PromoApp,
    state: ActionState,
    colors: PromoColors,
    showSeparator: Boolean,
    onGet: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxHeight()
            .padding(start = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(
            url = app.iconUrl,
            size = 60.dp,
            radius = 14.dp,
            colors = colors,
            modifier = Modifier.border(0.5.dp, colors.separator, RoundedCornerShape(14.dp)),
        )
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .drawBehind {
                    if (showSeparator) {
                        val y = size.height - 0.25.dp.toPx()
                        drawLine(colors.separator, Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx())
                    }
                }
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AppName(app.name, colors, Modifier.weight(1f, fill = false))
                    if (app.isNew) {
                        Box(Modifier.size(6.dp).background(colors.accent, CircleShape))
                    }
                }
                app.description?.let {
                    BasicText(
                        it,
                        style = TextStyle(
                            color = colors.label2,
                            fontSize = 13.sp,
                            lineHeight = 1.3.em,
                            // Texto do catálogo pode estar em outro idioma (en num app em árabe)
                            textDirection = TextDirection.Content,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            ActionButton(state, colors, onGet, onOpen)
        }
    }
}

@Composable
private fun AppName(name: String, colors: PromoColors, modifier: Modifier = Modifier) {
    BasicText(
        name,
        modifier = modifier,
        style = TextStyle(
            color = colors.label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = tracking(-0.01f),
            textDirection = TextDirection.Content,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun AppIcon(
    url: String?,
    size: Dp,
    radius: Dp,
    colors: PromoColors,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val inPreview = LocalInspectionMode.current
    val bitmap by produceState(initialValue = url?.let(IconLoader::cached), url) {
        if (url != null && value == null && !inPreview) value = IconLoader.load(context, url)
    }
    val shape = RoundedCornerShape(radius)
    Box(modifier.size(size).clip(shape)) {
        val loaded = bitmap
        if (loaded != null) {
            Image(loaded, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            // Ícone ainda baixando: mesmo pulso do skeleton, para não parecer quebrado
            val pulse = rememberInfiniteTransition(label = "icon").animateFloat(
                initialValue = 1f,
                targetValue = 0.45f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "pulse",
            )
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = pulse.value }.background(colors.fill))
        }
    }
}

@Composable
private fun ActionButton(
    state: ActionState,
    colors: PromoColors,
    onGet: () -> Unit,
    onOpen: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val onClick = when (state) {
        ActionState.Get -> onGet
        ActionState.Open -> onOpen
        ActionState.Loading -> null
    }
    Box(
        Modifier
            .height(44.dp)
            .widthIn(min = 78.dp)
            .then(
                if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state == ActionState.Loading) {
            Spinner(colors, 24.dp)
        } else {
            val open = state == ActionState.Open
            BasicText(
                stringResource(if (open) R.string.jalloft_promo_open else R.string.jalloft_promo_get),
                style = TextStyle(
                    color = if (open) Color.White else colors.accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                modifier = Modifier
                    .alpha(if (pressed) 0.5f else 1f)
                    .background(if (open) colors.accent else colors.fill, RoundedCornerShape(15.dp))
                    .heightIn(min = 30.dp)
                    .widthIn(min = 78.dp)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun Spinner(colors: PromoColors, size: Dp) {
    val rotation by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label = "rotation",
    )
    Canvas(Modifier.size(size).rotate(rotation)) {
        val stroke = 2.5.dp.toPx()
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(colors.fill, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
        drawArc(colors.accent, -135f, 90f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
    }
}

/**
 * Primeira abertura (sem cache): a silhueta da tela pulsando no lugar do
 * conteúdo, com a mesma geometria dos cards e linhas reais.
 */
@Composable
private fun SkeletonContent(
    columns: Int,
    pad: Dp,
    featWidth: Dp,
    colors: PromoColors,
    header: @Composable () -> Unit,
) {
    val pulse = rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse",
    )
    // Lê o estado só na fase de desenho: o pulso não recompõe nada
    val pulsing = Modifier.graphicsLayer { alpha = pulse.value }
    val rows = List(6) { it }.chunked(columns)

    Column(Modifier.fillMaxSize()) {
        header() // mesma posição do título da lista carregada: nada "pula" quando os dados chegam
        Row(
            pulsing
                .fillMaxWidth()
                .padding(start = pad, end = pad, top = 22.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBar(110.dp, 18.dp, colors)
            SkeletonBar(48.dp, 13.dp, colors)
        }
        LazyRow(
            modifier = pulsing,
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = pad),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(3) {
                Column(Modifier.width(featWidth).clip(RoundedCornerShape(16.dp)).background(colors.card)) {
                    Box(
                        Modifier.fillMaxWidth().height(150.dp).background(colors.fill),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(92.dp).background(colors.fill, RoundedCornerShape(21.dp)))
                    }
                    Row(
                        Modifier.padding(start = 16.dp, end = 14.dp, top = 17.dp, bottom = 17.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonBar(80.dp, 9.dp, colors)
                            SkeletonBar(150.dp, 14.dp, colors)
                        }
                        SkeletonBar(78.dp, 30.dp, colors)
                    }
                }
            }
        }

        SectionTitle(
            stringResource(R.string.jalloft_promo_all),
            colors,
            Modifier.padding(start = pad, end = pad, top = 26.dp, bottom = 10.dp),
        )

        Column(
            pulsing
                .padding(horizontal = pad)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.card)
        ) {
            rows.forEachIndexed { i, row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach {
                        SkeletonRow(colors, showSeparator = i != rows.lastIndex, Modifier.weight(1f))
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun SkeletonRow(colors: PromoColors, showSeparator: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier.padding(start = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(60.dp).background(colors.fill, RoundedCornerShape(14.dp)))
        Row(
            Modifier
                .weight(1f)
                .drawBehind {
                    if (showSeparator) {
                        val y = size.height - 0.25.dp.toPx()
                        drawLine(colors.separator, Offset(0f, y), Offset(size.width, y), 0.5.dp.toPx())
                    }
                }
                .padding(vertical = 19.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                SkeletonBar(130.dp, 14.dp, colors)
                SkeletonBar(170.dp, 10.dp, colors)
                SkeletonBar(110.dp, 10.dp, colors)
            }
            SkeletonBar(78.dp, 30.dp, colors)
        }
    }
}

@Composable
private fun SkeletonBar(width: Dp, height: Dp, colors: PromoColors) {
    Box(
        Modifier
            .size(width, height)
            .background(colors.fill, RoundedCornerShape(height / 2))
    )
}

@Composable
private fun ErrorState(colors: PromoColors, onRetry: () -> Unit, header: @Composable () -> Unit) {
    header()
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(
            stringResource(R.string.jalloft_promo_error),
            style = TextStyle(color = colors.label2, fontSize = 15.sp, textAlign = TextAlign.Center),
        )
        BasicText(
            stringResource(R.string.jalloft_promo_retry),
            style = TextStyle(color = colors.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(15.dp))
                .clickable(onClick = onRetry)
                .background(colors.fill)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Toast(text: String?, colors: PromoColors, onShown: () -> Unit, modifier: Modifier = Modifier) {
    // Mantém o último texto durante a animação de saída
    var lastText by remember { mutableStateOf(text.orEmpty()) }
    if (text != null) lastText = text
    LaunchedEffect(text) {
        if (text != null) {
            delay(1600)
            onShown()
        }
    }
    AnimatedVisibility(
        visible = text != null,
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 3 },
        exit = fadeOut(tween(200)),
        modifier = modifier,
    ) {
        BasicText(
            lastText,
            style = TextStyle(color = colors.toastText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(22.dp))
                .background(colors.toast, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
        )
    }
}

@Composable
@ReadOnlyComposable
private fun mono(family: FontFamily, size: TextUnit, weight: FontWeight, color: Color) = TextStyle(
    fontFamily = family,
    fontSize = size,
    fontWeight = weight,
    letterSpacing = tracking(0.08f),
    color = color,
)

/**
 * Espaçamento entre letras do design — só em LTR. O Android não aplica tracking
 * em escrita cursiva (árabe), mas o Compose mede o texto como se aplicasse: a
 * largura reservada fica menor que a real e a última letra quebra de linha.
 */
@Composable
@ReadOnlyComposable
private fun tracking(em: Float): TextUnit =
    if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextUnit.Unspecified else em.em

/**
 * Fundo do banner: gradiente a 135° + faixa diagonal da cor de destaque,
 * igual ao `linear-gradient(135deg, …)` do CSS do design.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBanner(colors: PromoColors) {
    val w = size.width
    val h = size.height
    // Linha do gradiente CSS a 135°: passa pelo centro, comprimento (w + h)·√2/2
    val half = (w + h) * (sqrt(2f) / 2f) / 2f
    val dir = Offset(sqrt(2f) / 2f, sqrt(2f) / 2f)
    val center = Offset(w / 2f, h / 2f)
    val start = center - dir * half
    val end = center + dir * half

    drawRect(
        Brush.linearGradient(
            0f to colors.bannerStart,
            0.65f to colors.bannerEnd,
            1f to colors.bannerEnd,
            start = start,
            end = end,
        )
    )
    drawRect(
        Brush.linearGradient(
            0.700f to Color.Transparent,
            0.700f to colors.bannerStripe,
            0.725f to colors.bannerStripe,
            0.725f to Color.Transparent,
            start = start,
            end = end,
        )
    )
}

// ---------------------------------------------------------------- previews

private val PreviewApps = listOf(
    PromoApp("com.jalloft.toondance", "ToonDance: 3D Dance Videos", null, "Entretenimento",
        "Personagens 3D dançam qualquer música. Crie vídeos, GIFs e figurinhas.", true, ""),
    PromoApp("com.jalloft.saturndraw", "SaturnDraw: Sketch & Paint", null, "Arte e design",
        "Esboce, entinte e pinte com pincéis reais, textura de papel, camadas e timelapse.", true, ""),
    PromoApp("com.jalloft.ninoclip", "NinoClip: Draw & Animate", null, "Arte e design",
        "Desenhe com pincéis reais, anime quadro a quadro e publique na comunidade.", true, ""),
    PromoApp("com.jalloft.elobiblia", "Elo Bíblia", null, "Estilo de vida",
        "Sua jornada de fé potencializada por IA, devocionais e estudos profundos.", false, ""),
    PromoApp("com.jalloft.sortix", "Sortix", null, "Social",
        "Organize seu amigo secreto rápido e com sigilo total para o organizador.", false, ""),
)

@Composable
private fun PreviewScreen(colors: PromoColors) {
    MoreAppsContent(
        apps = PreviewApps,
        failed = false,
        installed = setOf("com.jalloft.sortix"),
        pending = setOf("com.jalloft.elobiblia"),
        toast = null,
        colors = colors,
        monoFontFamily = FontFamily.Monospace,
        showFeatured = true,
        onBack = {},
        onRetry = {},
        onToastShown = {},
        onGet = {},
        onOpen = {},
    )
}

@Preview(name = "Claro", widthDp = 390, heightDp = 844, locale = "pt")
@Composable
private fun MoreAppsLightPreview() = PreviewScreen(PromoColors.Light)

@Preview(name = "Escuro", widthDp = 390, heightDp = 844, locale = "pt")
@Composable
private fun MoreAppsDarkPreview() = PreviewScreen(PromoColors.Dark)

@Preview(name = "Carregando", widthDp = 390, heightDp = 844, locale = "pt")
@Composable
private fun MoreAppsSkeletonPreview() = MoreAppsContent(
    apps = null, failed = false, installed = emptySet(), pending = emptySet(), toast = null,
    colors = PromoColors.Light, monoFontFamily = FontFamily.Monospace, showFeatured = true,
    onBack = {}, onRetry = {}, onToastShown = {}, onGet = {}, onOpen = {},
)

@Preview(name = "Tablet", widthDp = 1180, heightDp = 820, locale = "pt")
@Composable
private fun MoreAppsTabletPreview() = PreviewScreen(PromoColors.Light)
