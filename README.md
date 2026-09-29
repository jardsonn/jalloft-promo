# jalloft-promo

Tela "Mais apps para você": divulgação cruzada dos apps da Jalloft dentro de
cada app. O catálogo vem de `https://jalloft.com/api/apps` (cadastro no Sanity
do site), então app novo publicado no Studio aparece em todos os apps sem
atualizar nenhum deles.

- O app atual some da lista automaticamente.
- Lançamentos (`isNew` no Studio) num carrossel no topo; o resto numa lista.
- Botão Baixar → Play Store (com referrer UTM) → volta como **Abrir** se instalou.
- Claro/escuro, en (padrão) / pt / es / fr / it / ko / ar (RTL) / hi / fil / ms, 1 coluna no celular, 2–3 no tablet.
- Skeleton na primeira abertura; depois abre na hora com o cache do aparelho.
- Sem dependências além de Compose (sem Coil/Retrofit/Hilt).

## Instalação

`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

`app/build.gradle.kts`:

```kotlin
implementation("com.github.jardsonn:jalloft-promo:1.0.1")
```

## Uso

**Qualquer app (XML ou Compose)** — abre a tela como Activity:

```kotlin
JalloftPromo.open(context)
```

**Compose, dentro da própria navegação:**

```kotlin
composable("more-apps") {
    MoreAppsScreen(onBack = { navController.popBackStack() })
}
```

Texto pronto para o botão/item de menu: `R.string.jalloft_promo_entry`
("More apps" / "Mais apps" / "Más apps").

### Opcional

```kotlin
// Analytics: qual app foi tocado a partir deste
JalloftPromo.onAppClick = { packageName, installed ->
    Firebase.analytics.logEvent("cross_promo_click") {
        param("target", packageName)
        param("installed", if (installed) 1L else 0L)
    }
}

// Rótulos em caixa-alta com a fonte do design (JetBrains Mono), se o app já a tiver
JalloftPromo.monoFontFamily = FontFamily(Font(R.font.jetbrains_mono_bold, FontWeight.Bold))
```

## Pro / sem anúncios

A tela só abre quando a pessoa toca num botão ou item de menu, não usa rede de
anúncios e não rastreia nada — não é anúncio, então **fica visível também para
quem comprou a remoção de anúncios**. Qualquer divulgação que apareça sozinha
(card na tela inicial, diálogo, banner dos outros apps) passa a ser anúncio e
tem que sumir para quem é Pro.

Apps com App Open ad: Baixar/Abrir tiram a pessoa do app, e a volta dispararia
o anúncio de abertura. Suprima no `onAppClick`:

```kotlin
MoreAppsScreen(
    onBack = { navController.popBackStack() },
    onAppClick = { _, _ -> appOpenAds.suppressNextOpen() },
)
```

## Medindo

Os links para a Play levam `utm_source=<package do app de origem>&utm_medium=cross_promo`.
Veja em Play Console → Aquisição de usuários → Origens de tráfego.

## App novo

1. Importe o app no Studio do site (URL da Play → "Importar da Play Store") e publique.
2. Acrescente o package em `promo/src/main/AndroidManifest.xml` (`<queries>`) e
   publique uma versão nova da lib — sem isso o app aparece normalmente, mas o
   botão fica sempre em "Baixar" mesmo quando ele já está instalado.

## Publicar versão

Suba a versão em `promo/build.gradle.kts`, faça commit e crie a tag:

```bash
git tag 1.0.1 && git push origin main --tags
```

O JitPack compila na primeira vez que alguém pedir a versão
(log em `https://jitpack.io/#jardsonn/jalloft-promo`).

## Testar local

```bash
./gradlew :sample:installDebug -Pdev
```

Com `-Pdev` o sample lê o catálogo do `npm run dev` do site (`10.0.2.2:3000` no emulador).
