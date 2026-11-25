package id.kromaklub;

final class Palettes {
    record Swatch(String name, int rgb) {}
    record Palette(String name, String description, Swatch... colors) {}

    static final Palette[] ALL = {
        new Palette("Spektrum", "Warna cerah sehari-hari",
            swatch("Merah cabai", 0xED354A), swatch("Biru elektrik", 0x3775F5),
            swatch("Kuning lemon", 0xF5D740), swatch("Hijau apel", 0x50BD52),
            swatch("Ungu anggur", 0x8F56CC),
            swatch("Jingga jeruk", 0xFF841F),
            swatch("Merah marun", 0x8D2435),
            swatch("Merah muda fuksia", 0xDE439C),
            swatch("Hijau zamrud", 0x148A60),
            swatch("Biru kobalt", 0x2851AE),
            swatch("Ungu terung", 0x623A80),
            swatch("Kuning emas", 0xF1B52C),
            swatch("Toska terang", 0x20CEB2),
            swatch("Hijau limau", 0x96D844),
            swatch("Putih gading", 0xFFF9E9),
            swatch("Hitam arang", 0x303037)),
        new Palette("Pesisir", "Nuansa pantai dan laut",
            swatch("Biru laguna", 0x119DAD), swatch("Jingga karang", 0xF37C63),
            swatch("Kuning surya", 0xFFBD4A), swatch("Pasir hangat", 0xDAB88D),
            swatch("Biru samudra", 0x2D68A0),
            swatch("Hijau rumput laut", 0x48765C),
            swatch("Putih buih", 0xF4FBF9),
            swatch("Cokelat kerang", 0xAD826B),
            swatch("Biru langit", 0x85C9EC),
            swatch("Hijau kaca laut", 0x81C9AE),
            swatch("Merah udang", 0xE35A50),
            swatch("Biru senja", 0x566784),
            swatch("Abu-abu batu", 0x91969D),
            swatch("Kuning nanas", 0xF5CF54),
            swatch("Hijau kelapa", 0x709649),
            swatch("Jingga mentari", 0xF99D59)),
        new Palette("Pasar Sore", "Warna hangat dan membumi",
            swatch("Merah bata", 0xAB4839), swatch("Hijau zaitun", 0x808B46),
            swatch("Kuning kunyit", 0xD6963E), swatch("Cokelat kayu", 0x865845),
            swatch("Krem kanvas", 0xEDDBAB),
            swatch("Merah tomat", 0xD94735),
            swatch("Cokelat kopi", 0x573B30),
            swatch("Hijau pandan", 0x398956),
            swatch("Ungu manggis", 0x673850),
            swatch("Merah jambu biji", 0xEF8589),
            swatch("Jingga labu", 0xDE7833),
            swatch("Hijau serai", 0xA3AE64),
            swatch("Cokelat pala", 0xA46B44),
            swatch("Kuning jagung", 0xEFC347),
            swatch("Hijau daun pisang", 0x427240),
            swatch("Ungu ubi", 0x945B99)),
        new Palette("Awan Gula", "Palet pastel yang lembut",
            swatch("Merah muda permen", 0xF3ABC5), swatch("Ungu lilac", 0xBEABEF),
            swatch("Hijau mint", 0xAFDCC8), swatch("Biru kapas", 0xABCEF5),
            swatch("Persik muda", 0xF4C49E),
            swatch("Kuning vanila", 0xF9E6A2),
            swatch("Ungu periwinkle", 0xB1B7EF),
            swatch("Merah muda mawar", 0xE7A5B2),
            swatch("Hijau pistacio", 0xCADD9E),
            swatch("Krem biskuit", 0xEAD3B2),
            swatch("Biru es", 0xC4E9EE),
            swatch("Ungu kelopak", 0xD8A8DA),
            swatch("Jingga melon", 0xFFBD96),
            swatch("Kuning mentega", 0xF8ECA9),
            swatch("Merah muda kapas", 0xFFD4E4),
            swatch("Hijau embun", 0xC0E8CC))
    };

    private static Swatch swatch(String name, int rgb) {
        return new Swatch(name, 0xFF000000 | rgb);
    }

    private Palettes() {}
}
