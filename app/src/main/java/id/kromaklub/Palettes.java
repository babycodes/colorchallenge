package id.kromaklub;

final class Palettes {
    record Swatch(String name, int rgb) {}
    record Palette(String name, String description, Swatch... colors) {}

    static final Palette[] ALL = {
        new Palette("Spektrum", "Warna cerah sehari-hari",
            swatch("Merah cabai", 0xED354A), swatch("Biru elektrik", 0x3775F5),
            swatch("Kuning lemon", 0xF5D740), swatch("Hijau apel", 0x50BD52),
            swatch("Ungu anggur", 0x8F56CC)),
        new Palette("Pesisir", "Nuansa pantai dan laut",
            swatch("Biru laguna", 0x119DAD), swatch("Jingga karang", 0xF37C63),
            swatch("Kuning surya", 0xFFBD4A), swatch("Pasir hangat", 0xDAB88D),
            swatch("Biru samudra", 0x2D68A0)),
        new Palette("Pasar Sore", "Warna hangat dan membumi",
            swatch("Merah bata", 0xAB4839), swatch("Hijau zaitun", 0x808B46),
            swatch("Kuning kunyit", 0xD6963E), swatch("Cokelat kayu", 0x865845),
            swatch("Krem kanvas", 0xEDDBAB)),
        new Palette("Awan Gula", "Palet pastel yang lembut",
            swatch("Merah muda permen", 0xF3ABC5), swatch("Ungu lilac", 0xBEABEF),
            swatch("Hijau mint", 0xAFDCC8), swatch("Biru kapas", 0xABCEF5),
            swatch("Persik muda", 0xF4C49E))
    };

    private static Swatch swatch(String name, int rgb) {
        return new Swatch(name, 0xFF000000 | rgb);
    }

    private Palettes() {}
}
