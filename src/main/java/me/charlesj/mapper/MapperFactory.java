package me.charlesj.mapper;

/**
 * Create mapper from given id from iNES file.
 */
public class MapperFactory {
    public static Mapper createMapperFromId(int id) {
        switch (id) {
            case 0:
                return new NROM();
            case 2:
                return new UxROM();
            case 4:
                return new MMC3();
            case 22:
                return new VRC2(VRC2.A);
            case 23:
                return new VRC2(VRC2.B);
            default:
                return null;
        }
    }
}
